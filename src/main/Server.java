import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider.HashMethod;

public class Server {
	
	private Keys key = null;
	private EccEnc eccEnc = null;
	private Network network = new Network();
	// private static boolean isFiltered = false;
	private static final String DB_NAME = "server/serverDB" + Params.server_size + ".txt";
	private static final String FILTERED_DB = "server/Filtered_clientDB" + Params.client_size + ".txt";
	private static final String PAIR_DB_NAME = "server/Pair_DB" + Params.server_size + ".txt";
	private static final String SERVER_CIPHER = "server/Server_Cipher" + Params.server_size + ".txt"; 
	
	public static void main(String[] args) throws ClassNotFoundException {
		Server server = new Server();
		DB.generateServerDB();//生成服务端数据集
		//1. 密钥生成
		switch (Params.protocol) {
		case Unbalanced://非平衡
			server.key = CommEnc.generate_Key();//密钥管理对象
			if(server.key == null) {//密钥生成失败
				System.out.println("密钥生成失败");
				return;
			}
			break;
		case Reversed://反向非平衡
			server.key = CommEnc.generate_Key();//密钥管理对象
			if(server.key == null) {//密钥生成失败
				System.out.println("密钥生成失败");
				return;
			}
			break;
		default:
			break;
		}
		//2.打开监听端口，将模数p发送给Client
		server.connect();//服务端初始化
		while(true) {
			server.start();
		}
	}

	//服务端开启
	public void connect() {
		try {
			network.serverSock = new ServerSocket(8088);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("服务端初始化成功");
	}
	
	//启动监听
	public void start() throws ClassNotFoundException {
		try {
			network.sock = network.serverSock.accept();
			String address = network.sock.getInetAddress().getHostAddress();
			System.out.println("客户端ip为：" + address);
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
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 基于PIR的预过滤方法，小集合为过滤方，大集合将前缀发送给小集合一方过滤
	 * 先判定自己是否为小集合如果自己的集合大小小于等于2^16，则前缀16bit，如果大于2^16，则log(n)+1
	 * 目前先固定前缀长度为16bit，固定调用该函数的为大集合一方
	 * @param network, key
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
	 * 普通的哈希前缀预过滤，大集合为过滤方，小集合将前缀集合发送给大集合一方过滤
	 * 先判定自己是否为小集合，然后判定前缀长度是否需要调整，集合大小小于等于2^16，则前缀16bit，如果大于2^16，则log(n)+1
	 * 目前先固定前缀长度为16比特，固定调用该函数的为小集合一方
	 * @param network
	 * @param enc
	 * @return
	 */
	public boolean Pre_Filter(Network network) {//根据对方元素数量的数量级以及自己的元素数量的数量级差异，决定要生成的前缀长度，并产生相应的BF
		try {
			network.d_out = new DataOutputStream(network.sock.getOutputStream());
			// network.d_out.writeBoolean(false);
			// 1.对元素计算前缀、加密、生成二元组
			BloomFilter<String> prefixBloomFilter = new FilterBuilder()
					.expectedElements((int)Math.pow(2, Params.prefix_len))
					.falsePositiveProbability(0.000000001)
					.hashFunction(HashMethod.Murmur3)
					.buildBloomFilter();
			Utils.hash_prefix_enc_mThreads(Integer.valueOf(0), DB_NAME, "server/server_cipher.txt", prefixBloomFilter, Params.THREADS);
			// 2.发送前缀的布隆过滤器给client
			network.sendFile("server/server_prefix_bloom");
		} catch (IOException e) {
			e.printStackTrace();			
		}
		return true;
	}
	
	/**
	 * 基于PIR的预过滤方法，小集合接收来自大集合的前缀，在本地进行过滤
	 * 接收大集合一方发来的二元组，在本地进行过滤
	 * @param network
	 */
	public void Recv_Pir_Filter(Network network) {//接收对方发来的BF, 并接收对方的元素前缀长度.
    	try {
			// 1.接收对方的二元组集合
			network.d_in = new DataInputStream(network.sock.getInputStream());
			System.out.println("等待大集合一方的二元组集合...");
			network.receiveFile("server/server_pair_set");
			// 2.对本地元素生成前缀的布隆过滤器
			BloomFilter<String> filter = Utils.get_prefix_BF_mThreads(1, DB_NAME, Params.THREADS);
			// 3.对二元组集合进行过滤，生成新的数据集
			Utils.filter_Pair_Set(filter, "server/server_pair_set", FILTERED_DB);
			
			System.out.println("二元组集合过滤完毕!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void PSI_Rev(Network network) {//反向非平衡PSI-CA协议
		try {
			//1.发送素数p给客户端
			network.os = network.sock.getOutputStream();
			network.sendBI(key.getP());
			System.out.println("1.服务端已发送素数P");
			
			if(Params.pirFilter) {//使用PIR预过滤
				//2.Server接收来自Client的二元组集合并过滤存入本地
				Recv_Pir_Filter(network);
			} else if (Params.preFilter) {//使用预过滤
				Pre_Filter(network);
			}
			//2.Server将自己的元素加密，存入本地
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			System.out.println("服务端开始加密元素！");
			//多线程加密
			Utils.enc_dec_and_Write_mThreads(0, true, DB_NAME, "server/server_cipher.txt", key, Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("第2步服务端加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("2.Server将自己的元素加密，存入本地server_cipher.txt，并发送给Client");
			
			//3.Server把密文发送给client
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.sendFile("server/server_cipher.txt");//发送文件
				long local_endTime = System.currentTimeMillis();
				System.out.println("第3步服务端发送加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				System.out.println("3.Server将密文server_cipher.txt发送给Client");
			}).start();
			
			//4.等待客户端发送密文 接收客户端发送的密文文件
			if(!Params.pirFilter) {//不使用预过滤时才会到这一步
				System.out.println("4.Server开始接收Client的密文文件，并存入recv_client_cipher.txt中");
				CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
				new Thread(() -> {
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("server/recv_client_cipher.txt");
					long local_endTime = System.currentTimeMillis();
					System.out.println("第4步服务端接收加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdlCountDownLatch.countDown();
					System.out.println("4.Server已收到Client的文件，并存入recv_client_cipher.txt中");
				}).start();
				try {
					cdlCountDownLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//5.对客户端的密文文件进行二次加密，并将密文存入布谷鸟过滤器中
			//第3个参数为线程数量，即4个线程并发加密并插入CF
			online_startTime = System.currentTimeMillis();//线上阶段开始
			temp_startTime = System.currentTimeMillis();
			
			if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {//如果是布谷鸟过滤器
				// 使用与不使用预过滤的密文文件不同
				CuckooFilter<byte[]> filter;
				if(Params.pirFilter) {
					filter = Utils.sec_encrypt_and_CuckooWriter(0, FILTERED_DB, key, Params.THREADS);
				} else {
					filter = Utils.sec_encrypt_and_CuckooWriter(0, "server/recv_client_cipher.txt", key, Params.THREADS);
				}
				
				Utils.FilterWriter(filter, "server/server_cuckoo");
				endTime = System.currentTimeMillis();
				System.out.println("第5步服务端加密元素并存入CF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("5.Server将收到的密文进行二次加密，并写入filter中");
				
				//6.布谷鸟过滤器生成完毕，发回给客户端
				temp_startTime = System.currentTimeMillis();
				network.sendFile("server/server_cuckoo");//发送文件
				endTime = System.currentTimeMillis();
				System.out.println("第6步服务端发送CF给客户端耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Server布谷鸟过滤器生成完毕，并将CF发回给客户端");
			
			} else {//如果是布隆过滤器
				BloomFilter<String> bf;
				if(Params.pirFilter) {
					bf = Utils.encrypt_and_BloomWriter(0, FILTERED_DB, key, Params.THREADS);
				} else {
					bf = Utils.encrypt_and_BloomWriter(0, "server/recv_client_cipher.txt", key, Params.THREADS);
				}
				Utils.FilterWriter(bf, "server/server_bloom");
				endTime = System.currentTimeMillis();
				System.out.println("第5步服务端加密元素并存入BF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("5.Server将收到的密文进行二次加密，并写入filter中");
				
				//6.布隆过滤器生成完毕，发回给客户端
				temp_startTime = System.currentTimeMillis();
				network.sendFile("server/server_bloom");//发送文件
				endTime = System.currentTimeMillis();
				System.out.println("第6步服务端发送BF给客户端耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Server布隆过滤器生成完毕，并将BF发回给客户端");
			}
			
			System.out.println("服务端执行rev-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("服务端执行rev-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void ECC_PSI_Rev(Network network) {//反向非平衡PSI-CA协议
		long startTime = System.currentTimeMillis(); //获取开始时间
		long online_startTime;
		long endTime;
		long temp_startTime;
		//1.初始化椭圆曲线及密钥
		eccEnc = new EccEnc();
		
		if(Params.pirFilter) {//使用预过滤
			//Server接收来自Client的二元组集合并过滤存入本地
			Recv_Pir_Filter(network);
		} else if (Params.preFilter) {//使用预过滤
			Pre_Filter(network);
		}
		
		//2.服务端将自己的元素加密，存入本地
		try {
			Utils.ECC_enc_dec_and_Write_mThreads(eccEnc, 0, true, DB_NAME, "server/server_cipher.txt", Params.THREADS);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		endTime = System.currentTimeMillis();
		System.out.println("第2步服务端加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
		System.out.println("2.Server将自己的元素加密，存入本地server_cipher.txt，并发送给Client");
		
		online_startTime = System.currentTimeMillis();//线上阶段开始
		if(!Params.pirFilter) {
			//3.等待客户端发送密文 接收客户端发送的密文文件
			System.out.println("3.Server开始接收Client的密文文件，并存入recv_client_cipher.txt中");
			CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("server/recv_client_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("第3步服务端接收加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdlCountDownLatch.countDown();
				System.out.println("3.Server已收到Client的文件，并存入recv_client_cipher.txt中");
			}).start();
			try {
				cdlCountDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//4.Server把密文发送给client
		new Thread(() -> {
			long local_startTime = System.currentTimeMillis();
			network.sendFile("server/server_cipher.txt");//发送文件
			long local_endTime = System.currentTimeMillis();
			System.out.println("第3步服务端发送加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
			System.out.println("3.Server将密文server_cipher.txt发送给Client");
		}).start();
		
		//5.对客户端的密文文件进行二次加密，并将密文存入布谷鸟过滤器中
		//第3个参数为线程数量，即4个线程并发加密并插入CF
		temp_startTime = System.currentTimeMillis();
		if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {//如果是布谷鸟过滤器
			System.out.println("服务端开始加密元素插入CF");
			CuckooFilter<byte[]> filter;
			if(Params.pirFilter) {
				filter = Utils.sec_encrypt_and_CuckooWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
			} else {
				filter = Utils.sec_encrypt_and_CuckooWriter(0, "server/recv_client_cipher.txt", eccEnc, Params.THREADS);
			}
			
			Utils.FilterWriter(filter, "server/server_cuckoo");
			endTime = System.currentTimeMillis();
			System.out.println("第5步服务端加密元素并存入CF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("5.Server将收到的密文进行二次加密，并写入filter中");
		
			//6.布谷鸟过滤器生成完毕，发回给客户端
			temp_startTime = System.currentTimeMillis();
			network.sendFile("server/server_cuckoo");//发送文件
			endTime = System.currentTimeMillis();
			System.out.println("第6步服务端发送CF给客户端耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("6.Server布谷鸟过滤器生成完毕，并将CF发回给客户端");
		} else {//如果是布隆过滤器
			System.out.println("服务端开始加密元素插入BF");
			BloomFilter<String> bf; 
			if(Params.pirFilter) {
				bf = Utils.sec_encrypt_and_BloomWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
			} else {
				bf = Utils.sec_encrypt_and_BloomWriter(0, "server/recv_client_cipher.txt", eccEnc, Params.THREADS);
			}
			
			Utils.FilterWriter(bf, "server/server_bloom");
			endTime = System.currentTimeMillis();
			System.out.println("第5步服务端加密元素并存入BF耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("5.Server将收到的密文进行二次加密，并写入filter中");
			
			//6.布隆过滤器生成完毕，发回给客户端
			temp_startTime = System.currentTimeMillis();
			network.sendFile("server/server_bloom");//发送文件
			endTime = System.currentTimeMillis();
			System.out.println("第6步服务端发送BF给客户端耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("6.Server布隆过滤器生成完毕，并将BF发回给客户端");
		}
		
		System.out.println("服务端执行ECC-rev-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
		System.out.println("服务端执行ECC-rev-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		
		//network.disconnect();
	}
	
	private void PSI_Unbalanced(Network network) {//普通非平衡PSI-CA协议
		try {
			//1.发送素数p给客户端
			network.os = network.sock.getOutputStream();
			network.sendBI(key.getP());
			System.out.println("1.服务端已发送素数P");
		
			//2.加密自己的元素，并存入布隆过滤器
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {//使用布隆过滤器
				
				BloomFilter<String> bf;
//				if(isFiltered) {
//					bf = Utils.encrypt_and_BloomWriter(0, FILTERED_DB, key, Params.THREADS);
//				} else {
//					
//				}
				bf = Utils.encrypt_and_BloomWriter(0, DB_NAME, key, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("第2步服务端加密元素并存入BF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				online_startTime = System.currentTimeMillis();
				//3.将布隆过滤器发送给客户端，同时等待接收客户端发来的加密元素，这里新建两个线程实现全双工通信
				new Thread(() ->  {//发送
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(bf, "server/server_bloom");
					network.sendFile("server/server_bloom");//发送文件
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步服务端将BF发送给客户端耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
				
			} else {//使用布谷鸟过滤器
				CuckooFilter<byte[]> filter;
//				if(isFiltered) {
//					filter = Utils.encrypt_and_CuckooWriter(0, FILTERED_DB, key, Params.THREADS);
//				} else {
//					
//				}
				filter = Utils.encrypt_and_CuckooWriter(0, DB_NAME, key, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("第2步服务端加密元素并存入CF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				online_startTime = System.currentTimeMillis();
				//3.将布谷鸟过滤器发送给客户端，同时等待接收客户端发来的加密元素，这里新建两个线程实现全双工通信
				new Thread(() ->  {//发送
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(filter, "server/server_cuckoo");
					network.sendFile("server/server_cuckoo");//发送文件
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步服务端将CF发送给客户端耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
			}
			
			final CountDownLatch cdl = new CountDownLatch(1);//参数为线程个数
			new Thread(() ->  {//接收
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("server/recv_client_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("第4步服务端接收客户端加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdl.countDown();//接收线程必须全部执行完毕再开始二次加密
			}).start();
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//4.接收客户端的加密元素后，对其进行二次加密，加密完毕后返回给客户端
			temp_startTime = System.currentTimeMillis();
			Utils.enc_dec_and_Write_mThreads(0, true, "server/recv_client_cipher.txt", "server/recv_client_cipher2.txt", key, Params.THREADS);
			network.sendFile("server/recv_client_cipher2.txt");//发送文件
			endTime = System.currentTimeMillis();
			System.out.println("第4步服务端二次加密元素并返回客户端耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("服务端执行Unbalanced-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("服务端执行Unbalanced-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void ECC_PSI_Unbalanced(Network network) {//非平衡PSI-CA协议
		try {
			long startTime = System.currentTimeMillis(); //获取开始时间
			long online_startTime;
			long endTime;
			long temp_startTime;
			//1.初始化椭圆曲线及密钥
			eccEnc = new EccEnc();
		
			//2.加密自己的元素，并存入布隆过滤器
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				BloomFilter<String> bf;
//				if(isFiltered) {
//					bf = Utils.encrypt_and_BloomWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
//				} else {
//					
//				}
				bf = Utils.encrypt_and_BloomWriter(0, DB_NAME, eccEnc, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("第2步服务端加密元素并存入BF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				//3.将布隆过滤器发送给客户端，同时等待接收客户端发来的加密元素，这里新建两个线程实现全双工通信
				new Thread(() ->  {//发送
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(bf, "server/server_bloom");
					network.sendFile("server/server_bloom");//发送文件
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步服务端将BF发送给客户端耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
			} else {
				CuckooFilter<byte[]> filter;
//				if(isFiltered) {
//					filter = Utils.encrypt_and_CuckooWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
//				} else {
//					filter = Utils.encrypt_and_CuckooWriter(0, DB_NAME, eccEnc, Params.THREADS);
//				}
				filter = Utils.encrypt_and_CuckooWriter(0, DB_NAME, eccEnc, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("第2步服务端加密元素并存入CF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				//3.将布谷鸟过滤器发送给客户端，同时等待接收客户端发来的加密元素，这里新建两个线程实现全双工通信
				new Thread(() ->  {//发送
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(filter, "server/server_cuckoo");
					network.sendFile("server/server_cuckoo");//发送文件
					long local_endTime = System.currentTimeMillis();
					System.out.println("第3步服务端将CF发送给客户端耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
			}
			
			final CountDownLatch cdl = new CountDownLatch(1);//参数为线程个数
			new Thread(() ->  {//接收
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("server/recv_client_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("第4步服务端接收客户端加密元素耗时:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdl.countDown();//接收线程必须全部执行完毕再开始二次加密
			}).start();
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			online_startTime = System.currentTimeMillis();
			
			//4.接收客户端的加密元素后，对其进行二次加密，加密完毕后返回给客户端
			temp_startTime = System.currentTimeMillis();
			Utils.ECC_sec_enc_and_Write_mThreads(eccEnc, 0, true, "server/recv_client_cipher.txt", "server/recv_client_cipher2.txt", Params.THREADS);
			network.sendFile("server/recv_client_cipher2.txt");//发送文件
			endTime = System.currentTimeMillis();
			System.out.println("第4步服务端二次加密元素并返回客户端耗时:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("服务端执行Unbalanced-PSI-CA总耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("服务端执行Unbalanced-PSI-CA线上阶段总耗时:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


//public void Recv_Pre_Filter(Network network) {//接收对方发来的BF, 并接收对方的元素前缀长度.
//try {
//	//先获取自己的元素数量
//	long nums = Params.server_size;
//	network.d_out = new DataOutputStream(network.sock.getOutputStream());
//	//将元素数量发送给对方
//	network.d_out.writeLong(nums);
//	
//	//接收对方的过滤意向
//	network.d_in = new DataInputStream(network.sock.getInputStream());
//	boolean isOK = network.d_in.readBoolean();
//	if(!isOK) {
//		System.out.println("对方拒绝数据过滤请求,开始正常的PSI-CA流程");
//		isFiltered = false;
//		return;
//	}
//	
//	//接收对方的前缀长度
//	isFiltered = true;
//	System.out.println("对方同意数据过滤请求,开始过滤");
//	network.d_in = new DataInputStream(network.sock.getInputStream());
//	int pre_len = network.d_in.readInt();
//	
//	//接收对方的BF
//	network.receiveFile("client/server_pre_bloom");
//	
//	BloomFilter<String> filter = new FilterBuilder()
//			.expectedElements(Params.client_size)
//			.falsePositiveProbability(0.0000000001)
//			.hashFunction(HashMethod.Murmur3)
//			.buildBloomFilter();
//	// BloomFilter<String> filter = Utils.BloomReader("client/server_pre_bloom");
//	
//	//开始读取本地文件中的元素，进行过滤，生成新的数据集
//	FileReader fileReader = new FileReader(DB_NAME);
//	BufferedReader bReader = new BufferedReader(fileReader);
//	FileWriter fileWriter = new FileWriter(FILTERED_DB_NAME);
//	BufferedWriter bWriter = new BufferedWriter(fileWriter);
//	
//	int cnt = 0;
//	String lineString = "";
//	byte[] result = null;
//	while((lineString = bReader.readLine()) != null) {
//		result = PreReduce.Hash_and_Get_Bits(lineString, pre_len);//哈希并截取前缀
//		
//		if(filter.contains(result)) {//如果元素前缀能匹配上, 就直接写到新文件中
//			bWriter.write(lineString + "\r\n");
//			cnt++;
//		}
//	}
//	System.out.println("前缀匹配上的元素数量:" + cnt);
//	bReader.close();
//	bWriter.close();
//} catch (IOException e) {
//	e.printStackTrace();
//}
//}

//public boolean Pre_Filter(Network network) {
//try {
//	network.d_in = new DataInputStream(network.sock.getInputStream());
//	long recv_nums = network.d_in.readLong();//文件总长度
//	//long order_gap = recv_nums / Params.client_size;//此时对方的数据量应该大于自己为宜
//	
//	int log_size_other = (int) PreReduce.log2(recv_nums);//对方的数据量可以向下取整
//	int log_size_local = (int) Math.ceil(PreReduce.log2(Params.client_size));//自己的数据量向上取整
//	
//	int diff = log_size_other - log_size_local;
//	
//	if(diff < 7) {
//		//不继续过滤，发送false
//		network.d_out = new DataOutputStream(network.sock.getOutputStream());
//		network.d_out.writeBoolean(false);
//		isFiltered = false;
//		System.out.println("数据量差距小于等于2^7, 不可以过滤");
//		return false;//如果数据量级差异小于2^7, 则认为过滤操作不安全, 自己的数据存在暴露风险, 应拒绝前缀过滤
//	} else {
//		//继续过滤，发送true
//		network.d_out = new DataOutputStream(network.sock.getOutputStream());
//		network.d_out.writeBoolean(true);
//		isFiltered = true;
//		//如果数据量级差异大于等于2^7, 则可以继续过滤
//		System.out.println("数据量差距大于等于2^7, 可以过滤");
//		int pre_len = log_size_local + diff - 7;//前缀长度自动补齐，保证log_size_other-pre_len = 7
//		//开始过滤
//		FileReader fileReader = new FileReader(DB_NAME);
//		BufferedReader bReader = new BufferedReader(fileReader);
//		BloomFilter<String> filter = new FilterBuilder()
//				.expectedElements(Params.client_size)
//				.falsePositiveProbability(0.0000000001)
//				.hashFunction(HashMethod.Murmur3)
//				.buildBloomFilter();
//		String lineString = "";
//		byte[] result;
//		//读取文件并截取前缀, 然后存入布隆过滤器
//		while ((lineString = bReader.readLine()) != null) {//读取文件
//			result = PreReduce.Hash_and_Get_Bits(lineString, pre_len);//哈希并截取前缀
//			filter.add(result);//存入BF
//		}
//		bReader.close();
//		//存入后将BF发送给服务端
//		//先将前缀长度发过去
//		network.d_out.writeInt(pre_len);
//		//再将BF发过去
//		Utils.FilterWriter(filter, "client/pre_bloom");
//		network.sendFile("client/pre_bloom");
//	}
//} catch (IOException e) {
//	e.printStackTrace();			
//}
//return true;
//}