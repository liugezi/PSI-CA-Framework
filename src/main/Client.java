import java.awt.RenderingHints.Key;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.annotation.concurrent.ThreadSafe;

import org.bouncycastle.crypto.DataLengthException;

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;
import com.github.mgunlogson.cuckoofilter4j.Utils.Algorithm;
import com.google.common.hash.Funnels;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider.HashMethod;

public class Client {
	private static Keys key = null;//密钥管理对象
	private EccEnc eccEnc = null;
	private static Network network = new Network();
	private static final String DB_NAME = "client/clientDB" + Params.client_size + ".txt";
	private static final String FILTERED_DB = "client/Filtered_serverDB" + Params.server_size + ".txt";
	private static final String PAIR_DB_NAME = "client/Pair_DB" + Params.client_size + ".txt";
	private static final String FILTERED_CLIENT_DB = "client/Filtered_clientDB" + Params.client_size + ".txt";
	
	public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		Client client = new Client();
		DB.generateClientDB();//生成客户端数据集
		client.connect();
	}
	
	public void connect() {
		try {
			network.sock = new Socket("localhost",8088);
			System.out.println("创建客户端成功");
			switch (Params.protocol) {
			case Unbalanced://非平衡
				PSI_Unbalanced(network);
				break;
			case Reversed://反向非平衡
				PSI_Rev(network);
				break;
			case ECC_Unbalanced://椭圆曲线非平衡
				ECC_PSI_Unbalanced(network);
				break;
			case ECC_Reversed://椭圆曲线反向非平衡
				ECC_PSI_Rev(network);
				break;
			default:
				break;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Client为大集合一方时，预过滤调用
	 * @param network
	 * @param enc Object类型，可能为Keys或EccEnc对象
	 * @return
	 */
	public boolean Pir_Filter(Network network, Object enc) {//根据对方元素数量的数量级以及自己的元素数量的数量级差异，决定要生成的前缀长度，并产生相应的BF
		try {
			network.d_out = new DataOutputStream(network.sock.getOutputStream());
			// network.d_out.writeBoolean(false);
			// 1.对元素计算前缀、加密、生成二元组
			Utils.hash_prefix_enc_mThreads(Integer.valueOf(1), DB_NAME, PAIR_DB_NAME, enc, Params.THREADS);
			// 2.发送二元组文件给Server
			network.sendFile(PAIR_DB_NAME);
		} catch (IOException e) {
			e.printStackTrace();			
		}
		return true;
	}
	
	/**
	 * Client为小集合一方时，接收Server发来的二元组集合进行过滤
	 * @param network
	 */
	public void Recv_Pir_Filter(Network network) {//接收对方发来的BF, 并接收对方的元素前缀长度.
    	try {
			// 1.接收对方的二元组集合
			network.d_in = new DataInputStream(network.sock.getInputStream());
			network.receiveFile("client/server_pair_set");
			// 2.对本地元素生成前缀的布隆过滤器
			BloomFilter<String> filter = Utils.get_prefix_BF_mThreads(1, DB_NAME, Params.THREADS);
			// 3.对二元组集合进行过滤，生成新的数据集
			Utils.filter_Pair_Set(filter, "client/server_pair_set", FILTERED_DB);
			
			System.out.println("二元组集合过滤完毕!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 普通的哈希前缀预过滤，大集合为过滤方，小集合将前缀集合发送给大集合一方过滤
	 * 先判定自己是否为小集合，然后判定前缀长度是否需要调整，集合大小小于等于2^16，则前缀16bit，如果大于2^16，则log(n)+1
	 * 目前先固定前缀长度为16比特，固定调用该函数的为小集合一方
	 * @param network
	 * @param enc
	 * @return
	 */
	public boolean Recv_Pre_Filter(Network network, Object enc) {//根据对方元素数量的数量级以及自己的元素数量的数量级差异，决定要生成的前缀长度，并产生相应的BF
		try {
			// 1.接收对方的布隆过滤器
			network.d_in = new DataInputStream(network.sock.getInputStream());
			network.receiveFile("client/recv_prefix_filter");
			BloomFilter<String> filter = Utils.BloomReader("client/recv_prefix_filter");//从文件中读取bf
			// 2.对本地元素生成前缀，并对布隆过滤器进行查询，存在的则加密并留下
			Utils.filter_Set_mThreads(1, filter, DB_NAME, FILTERED_CLIENT_DB, enc, Params.THREADS);
			System.out.println("集合过滤完毕!");
			// 3.发送过滤后的密文集合给Server
			network.sendFile(FILTERED_CLIENT_DB);
		} catch (IOException e) {
			e.printStackTrace();			
		}
		return true;
	}
	
	private void PSI_Rev(Network network) {//反向非平衡PSI-CA
		try {
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			network.is = network.sock.getInputStream();//拿到Server传递的字节流
			//将字节流转换为字符流
			//1.拿到素数P，初始化密钥Key
			byte[] read = network.readBytes();
			key = new Keys(Utils.bytesToBigInteger(read, 0, read.length));
			System.out.println("1.Client拿到素数P：" + key.getP().toString());
			
			CommEnc.generate_Keys(key);//初始化加密密钥和解密密钥
			
			if(Params.pirFilter) {// 使用过滤器
				//2.生成预过滤前缀二元组
				Pir_Filter(network, key);
			} else if (Params.preFilter) {
				Recv_Pre_Filter(network, key);
			} else {// 不使用过滤器
				//2.先加密，再发送密文给对方
				Utils.enc_dec_and_Write_mThreads(1, true, DB_NAME, "client/client_cipher.txt", key, Params.THREADS);
				endTime = System.currentTimeMillis();
				System.out.println("第2步客户端加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				System.out.println("2.Client对本地集合元素完成加密，并写入client_cipher中");
				new Thread(() -> {
					long local_startTime = System.currentTimeMillis();
					network.sendFile("client/client_cipher.txt");//发送文件
					long local_endTime = System.currentTimeMillis();
					System.out.println("第2步客户端发送加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					System.out.println("2.Client将client_cipher发送给server");
				}).start();
			}
			online_startTime = System.currentTimeMillis();//线上阶段开始
			//3.服务器将密文文件发送了过来
			CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("client/recv_server_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("第3步客户端接收加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdlCountDownLatch.countDown();
				System.out.println("3.Client接收来自Server的密文，并存入本地recv_server_cipher.txt");
			}).start();
			
			//保证密文文件发完才能开始后面的二次加密
			try {
				cdlCountDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//4.将密文文件二次加密保存至本地
			//多线程加密
			System.out.println("4.Client将来自Server的密文进行二次加密，并存入本地server_cipher.txt");
			temp_startTime = System.currentTimeMillis();
			Utils.enc_dec_and_Write_mThreads(1, true, "client/recv_server_cipher.txt", "client/server_cipher.txt", key, Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("第4步客户端二次加密Server密文元素耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//5.服务器将元素二次加密后，放入过滤器，又发了回来
			temp_startTime = System.currentTimeMillis();
			if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {
				network.receiveFile("client/client_cuckoo");//将cf存入本地
				System.out.println("5.Client接收来自Server的CF，并存入本地client_cuckoo");
				
				endTime = System.currentTimeMillis();
				System.out.println("第5步客户端接收CF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//6.用server_cipher.txt中的文件元素在cf中进行查询，求出交集大小
				temp_startTime = System.currentTimeMillis();
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//从文件中读取cf
				int cardinality = Utils.cuckoo_query_cardinality(false, "client/server_cipher.txt",filter);
				endTime = System.currentTimeMillis();
				System.out.println("第6步客户端读取CF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Client求出交集大小为:" + cardinality + "\n");
			} else {
				network.receiveFile("client/client_bloom");//将cf存入本地
				System.out.println("5.Client接收来自Server的BF，并存入本地client_bloom");
				
				endTime = System.currentTimeMillis();
				System.out.println("第5步客户端接收BF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//7.用server_cipher.txt中的文件元素在BF中进行查询，求出交集大小
				temp_startTime = System.currentTimeMillis();
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//从文件中读取bf
				int cardinality = Utils.bloom_query_cardinality("client/server_cipher.txt",filter);
				endTime = System.currentTimeMillis();
				System.out.println("第6步客户端读取BF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Client求出交集大小为:" + cardinality + "\n");
			}
			System.out.println("客户端执行Rev-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("客户端执行Rev-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
			network.disconnect();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void ECC_PSI_Rev(Network network) {//反向非平衡PSI-CA协议
		try {
			//1.初始化椭圆曲线及密钥
			eccEnc = new EccEnc();
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			if(Params.pirFilter) {// 使用过滤器
				//2.生成预过滤前缀二元组
				Pir_Filter(network, eccEnc);
			} else if (Params.preFilter) {
				Recv_Pre_Filter(network, eccEnc);
			} else {
				//2.开始对本地集合中的元素进行加密，并写入新的文件client_cipher中
				Utils.ECC_enc_dec_and_Write_mThreads(eccEnc, 1, true, DB_NAME, "client/client_cipher.txt", Params.THREADS);
				endTime = System.currentTimeMillis();
				System.out.println("第2步客户端加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				System.out.println("2.Client对本地集合元素完成加密，并写入client_cipher中");
				//3.client把密文发送给server
				new Thread(() -> {
					long local_startTime = System.currentTimeMillis();
					network.sendFile("client/client_cipher.txt");//发送文件
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步客户端发送加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					System.out.println("3.Client已将client_cipher.txt发送给server");
				}).start();
			}
			
			//4.服务器将密文文件发送了过来
			CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("client/recv_server_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("第4步接收服务端密文耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdlCountDownLatch.countDown();
				System.out.println("4.Client接收来自Server的密文，并存入本地recv_server_cipher.txt");
			}).start();
			
			//保证密文文件发完才能开始后面的二次加密
			try {
				cdlCountDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			online_startTime = System.currentTimeMillis();//线上阶段开始
			//5.将密文文件二次加密保存至本地
			//多线程加密
			System.out.println("5.Client将来自Server的密文进行二次加密，并存入本地server_cipher.txt");
			temp_startTime = System.currentTimeMillis();
			Utils.ECC_sec_enc_and_Write_mThreads(eccEnc, 1, true, "client/recv_server_cipher.txt", "client/server_cipher.txt", Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("第5步客户端二次加密Server密文元素耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//6.服务器将元素二次加密后，放入布谷鸟过滤器，又发了回来
			temp_startTime = System.currentTimeMillis();
			
			if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {
				network.receiveFile("client/client_cuckoo");//将cf存入本地
				System.out.println("6.Client接收来自Server的CF，并存入本地client_cuckoo");
				
				endTime = System.currentTimeMillis();
				System.out.println("第6步客户端接收CF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//7.用server_cipher.txt中的文件元素在cf中进行查询，求出交集大小
				temp_startTime = System.currentTimeMillis();
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//从文件中读取cf
				int cardinality = Utils.cuckoo_query_cardinality(true, "client/server_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("第7步客户端读取CF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client求出交集大小为:" + cardinality + "\n");
			} else {
				network.receiveFile("client/client_bloom");//将cf存入本地
				System.out.println("6.Client接收来自Server的BF，并存入本地client_bloom");
				
				endTime = System.currentTimeMillis();
				System.out.println("第6步客户端接收BF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//7.用server_cipher.txt中的文件元素在BF中进行查询，求出交集大小
				temp_startTime = System.currentTimeMillis();
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//从文件中读取bf
				int cardinality = Utils.bloom_query_cardinality("client/server_cipher.txt",filter);
				endTime = System.currentTimeMillis();
				System.out.println("第7步客户端读取BF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client求出交集大小为:" + cardinality + "\n");
			}
			System.out.println("客户端执行ECC-Rev-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("客户端执行ECC-Rev-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
			network.disconnect();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void PSI_Unbalanced(Network network) {//非平衡PSI
		try {
			network.is = network.sock.getInputStream();//拿到Server传递的字节流
			//将字节流转换为字符流
			//1.拿到素数P，初始化密钥Key
			byte[] read = network.readBytes();
			key = new Keys(Utils.bytesToBigInteger(read, 0, read.length));
			System.out.println("1.Client拿到素数P：" + key.getP().toString());
			CommEnc.generate_Keys(key);//初始化加密密钥和解密密钥
			
			//2.开始对本地集合中的元素进行加密，并写入新的文件client_cipher中
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			Utils.enc_dec_and_Write_mThreads(1, true, DB_NAME, "client/client_cipher.txt", key, Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("第2步客户端加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("2.Client对本地集合元素完成加密，并写入client_cipher中");
			
			
			//4.服务器将元素加密后，放入布隆过滤器，发了过来
			CountDownLatch cdl = new CountDownLatch(1);
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				new Thread(() ->  {//接收
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_bloom");
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步客户端接收BF耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("3.Client接收来自Server的BF，并存入本地client_bloom");
				}).start();
			} else {
				new Thread(() ->  {//接收
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_cuckoo");
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步客户端接收CF耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("3.Client接收来自Server的CF，并存入本地client_cuckoo");
				}).start();
			}
			
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//3.client把密文发送给server
			new Thread(() ->  {//发送
				long local_startTime = System.currentTimeMillis();
				network.sendFile("client/client_cipher.txt");//发送文件
				long local_endTime = System.currentTimeMillis();
				System.out.println("第4步客户端发送密文元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				System.out.println("4.Client将client_cipher发送给server");
			}).start();
			
			online_startTime = System.currentTimeMillis();
			//5.从服务端接收自己二次加密后的元素集合
			temp_startTime = System.currentTimeMillis();
			network.receiveFile("client/recv_client_cipher_2.txt");
			endTime = System.currentTimeMillis();
			System.out.println("第5步客户端从服务端接收二次加密后的元素耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//6.对接收到的密文进行去盲
			temp_startTime = System.currentTimeMillis();
			Utils.enc_dec_and_Write_mThreads(1, false, "client/recv_client_cipher_2.txt", "client/recv_client_decrypt_cipher.txt", key, Params.THREADS);	
			endTime = System.currentTimeMillis();
			System.out.println("第6步客户端对密文元素去盲耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//7.查询BF=>得到交集大小
			temp_startTime = System.currentTimeMillis();
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//从文件中读取bf
				int cardinality = Utils.bloom_query_cardinality("client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("第7步客户端查询BF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client求出交集大小为:" + cardinality + "\n");
			} else {
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//从文件中读取bf
				int cardinality = Utils.cuckoo_query_cardinality(false, "client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("第7步客户端查询CF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client求出交集大小为:" + cardinality + "\n");
			}
			System.out.println("客户端执行Unbalanced-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("客户端执行Unbalanced-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
	}
	
	private void ECC_PSI_Unbalanced(Network network) {//基于ECC的反向非平衡PSI-CA协议
		try {
			//1.初始化椭圆曲线及密钥
			eccEnc = new EccEnc();
			//2.开始对本地集合中的元素进行加密，并写入新的文件client_cipher中
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			Utils.ECC_enc_dec_and_Write_mThreads(eccEnc, 1, true, DB_NAME, "client/client_cipher.txt", Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("第2步客户端加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("2.Client对本地集合元素完成加密，并写入client_cipher中");
			
			//3.服务器将元素加密后，放入布隆过滤器，发了过来
			CountDownLatch cdl = new CountDownLatch(1);
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				new Thread(() ->  {//接收
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_bloom");
					long local_endTime = System.currentTimeMillis();
					System.out.println("第4步客户端接收BF耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("4.Client接收来自Server的BF，并存入本地client_bloom");
				}).start();
			} else {
				new Thread(() ->  {//接收
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_cuckoo");
					long local_endTime = System.currentTimeMillis();
					System.out.println("第4步客户端接收CF耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("4.Client接收来自Server的CF，并存入本地client_cuckoo");
				}).start();
			}
			
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//4.client把密文发送给server
			new Thread(() ->  {//发送
				long local_startTime = System.currentTimeMillis();
				network.sendFile("client/client_cipher.txt");//发送文件
				long local_endTime = System.currentTimeMillis();
				System.out.println("第3步客户端发送密文元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				System.out.println("3.Client将client_cipher发送给server");
			}).start();
			
			online_startTime = System.currentTimeMillis();
			//5.从服务端接收自己二次加密后的元素集合
			temp_startTime = System.currentTimeMillis();
			network.receiveFile("client/recv_client_cipher_2.txt");
			endTime = System.currentTimeMillis();
			System.out.println("第5步客户端从服务端接收二次加密后的元素耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//6.对接收到的密文进行去盲
			temp_startTime = System.currentTimeMillis();
			Utils.ECC_sec_enc_and_Write_mThreads(eccEnc, 1, false, "client/recv_client_cipher_2.txt", "client/recv_client_decrypt_cipher.txt", Params.THREADS);	
			endTime = System.currentTimeMillis();
			System.out.println("第6步客户端对密文元素去盲耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//7.查询BF=>得到交集大小
			temp_startTime = System.currentTimeMillis();
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//从文件中读取bf
				int cardinality = Utils.bloom_query_cardinality("client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("第7步客户端查询BF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client求出交集大小为:" + cardinality + "\n");
			} else {
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//从文件中读取bf
				int cardinality = Utils.cuckoo_query_cardinality(true, "client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("第7步客户端查询CF求交集大小耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client求出交集大小为:" + cardinality + "\n");
			}
			
			System.out.println("客户端执行Unbalanced-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("客户端执行Unbalanced-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}