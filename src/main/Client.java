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
	private static Keys key = null;//��Կ�������
	private EccEnc eccEnc = null;
	private static Network network = new Network();
	private static final String DB_NAME = "client/clientDB" + Params.client_size + ".txt";
	private static final String FILTERED_DB = "client/Filtered_serverDB" + Params.server_size + ".txt";
	private static final String PAIR_DB_NAME = "client/Pair_DB" + Params.client_size + ".txt";
	private static final String FILTERED_CLIENT_DB = "client/Filtered_clientDB" + Params.client_size + ".txt";
	
	public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		Client client = new Client();
		DB.generateClientDB();//���ɿͻ������ݼ�
		client.connect();
	}
	
	public void connect() {
		try {
			network.sock = new Socket("localhost",8088);
			System.out.println("�����ͻ��˳ɹ�");
			switch (Params.protocol) {
			case Unbalanced://��ƽ��
				PSI_Unbalanced(network);
				break;
			case Reversed://�����ƽ��
				PSI_Rev(network);
				break;
			case ECC_Unbalanced://��Բ���߷�ƽ��
				ECC_PSI_Unbalanced(network);
				break;
			case ECC_Reversed://��Բ���߷����ƽ��
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
	 * ClientΪ�󼯺�һ��ʱ��Ԥ���˵���
	 * @param network
	 * @param enc Object���ͣ�����ΪKeys��EccEnc����
	 * @return
	 */
	public boolean Pir_Filter(Network network, Object enc) {//���ݶԷ�Ԫ���������������Լ��Լ���Ԫ�����������������죬����Ҫ���ɵ�ǰ׺���ȣ���������Ӧ��BF
		try {
			network.d_out = new DataOutputStream(network.sock.getOutputStream());
			// network.d_out.writeBoolean(false);
			// 1.��Ԫ�ؼ���ǰ׺�����ܡ����ɶ�Ԫ��
			Utils.hash_prefix_enc_mThreads(Integer.valueOf(1), DB_NAME, PAIR_DB_NAME, enc, Params.THREADS);
			// 2.���Ͷ�Ԫ���ļ���Server
			network.sendFile(PAIR_DB_NAME);
		} catch (IOException e) {
			e.printStackTrace();			
		}
		return true;
	}
	
	/**
	 * ClientΪС����һ��ʱ������Server�����Ķ�Ԫ�鼯�Ͻ��й���
	 * @param network
	 */
	public void Recv_Pir_Filter(Network network) {//���նԷ�������BF, �����նԷ���Ԫ��ǰ׺����.
    	try {
			// 1.���նԷ��Ķ�Ԫ�鼯��
			network.d_in = new DataInputStream(network.sock.getInputStream());
			network.receiveFile("client/server_pair_set");
			// 2.�Ա���Ԫ������ǰ׺�Ĳ�¡������
			BloomFilter<String> filter = Utils.get_prefix_BF_mThreads(1, DB_NAME, Params.THREADS);
			// 3.�Զ�Ԫ�鼯�Ͻ��й��ˣ������µ����ݼ�
			Utils.filter_Pair_Set(filter, "client/server_pair_set", FILTERED_DB);
			
			System.out.println("��Ԫ�鼯�Ϲ������!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ͨ�Ĺ�ϣǰ׺Ԥ���ˣ��󼯺�Ϊ���˷���С���Ͻ�ǰ׺���Ϸ��͸��󼯺�һ������
	 * ���ж��Լ��Ƿ�ΪС���ϣ�Ȼ���ж�ǰ׺�����Ƿ���Ҫ���������ϴ�СС�ڵ���2^16����ǰ׺16bit���������2^16����log(n)+1
	 * Ŀǰ�ȹ̶�ǰ׺����Ϊ16���أ��̶����øú�����ΪС����һ��
	 * @param network
	 * @param enc
	 * @return
	 */
	public boolean Recv_Pre_Filter(Network network, Object enc) {//���ݶԷ�Ԫ���������������Լ��Լ���Ԫ�����������������죬����Ҫ���ɵ�ǰ׺���ȣ���������Ӧ��BF
		try {
			// 1.���նԷ��Ĳ�¡������
			network.d_in = new DataInputStream(network.sock.getInputStream());
			network.receiveFile("client/recv_prefix_filter");
			BloomFilter<String> filter = Utils.BloomReader("client/recv_prefix_filter");//���ļ��ж�ȡbf
			// 2.�Ա���Ԫ������ǰ׺�����Բ�¡���������в�ѯ�����ڵ�����ܲ�����
			Utils.filter_Set_mThreads(1, filter, DB_NAME, FILTERED_CLIENT_DB, enc, Params.THREADS);
			System.out.println("���Ϲ������!");
			// 3.���͹��˺�����ļ��ϸ�Server
			network.sendFile(FILTERED_CLIENT_DB);
		} catch (IOException e) {
			e.printStackTrace();			
		}
		return true;
	}
	
	private void PSI_Rev(Network network) {//�����ƽ��PSI-CA
		try {
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			network.is = network.sock.getInputStream();//�õ�Server���ݵ��ֽ���
			//���ֽ���ת��Ϊ�ַ���
			//1.�õ�����P����ʼ����ԿKey
			byte[] read = network.readBytes();
			key = new Keys(Utils.bytesToBigInteger(read, 0, read.length));
			System.out.println("1.Client�õ�����P��" + key.getP().toString());
			
			CommEnc.generate_Keys(key);//��ʼ��������Կ�ͽ�����Կ
			
			if(Params.pirFilter) {// ʹ�ù�����
				//2.����Ԥ����ǰ׺��Ԫ��
				Pir_Filter(network, key);
			} else if (Params.preFilter) {
				Recv_Pre_Filter(network, key);
			} else {// ��ʹ�ù�����
				//2.�ȼ��ܣ��ٷ������ĸ��Է�
				Utils.enc_dec_and_Write_mThreads(1, true, DB_NAME, "client/client_cipher.txt", key, Params.THREADS);
				endTime = System.currentTimeMillis();
				System.out.println("��2���ͻ��˼���Ԫ�غ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				System.out.println("2.Client�Ա��ؼ���Ԫ����ɼ��ܣ���д��client_cipher��");
				new Thread(() -> {
					long local_startTime = System.currentTimeMillis();
					network.sendFile("client/client_cipher.txt");//�����ļ�
					long local_endTime = System.currentTimeMillis();
					System.out.println("��2���ͻ��˷��ͼ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					System.out.println("2.Client��client_cipher���͸�server");
				}).start();
			}
			online_startTime = System.currentTimeMillis();//���Ͻ׶ο�ʼ
			//3.�������������ļ������˹���
			CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("client/recv_server_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("��3���ͻ��˽��ռ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdlCountDownLatch.countDown();
				System.out.println("3.Client��������Server�����ģ������뱾��recv_server_cipher.txt");
			}).start();
			
			//��֤�����ļ�������ܿ�ʼ����Ķ��μ���
			try {
				cdlCountDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//4.�������ļ����μ��ܱ���������
			//���̼߳���
			System.out.println("4.Client������Server�����Ľ��ж��μ��ܣ������뱾��server_cipher.txt");
			temp_startTime = System.currentTimeMillis();
			Utils.enc_dec_and_Write_mThreads(1, true, "client/recv_server_cipher.txt", "client/server_cipher.txt", key, Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("��4���ͻ��˶��μ���Server����Ԫ�غ�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//5.��������Ԫ�ض��μ��ܺ󣬷�����������ַ��˻���
			temp_startTime = System.currentTimeMillis();
			if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {
				network.receiveFile("client/client_cuckoo");//��cf���뱾��
				System.out.println("5.Client��������Server��CF�������뱾��client_cuckoo");
				
				endTime = System.currentTimeMillis();
				System.out.println("��5���ͻ��˽���CF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//6.��server_cipher.txt�е��ļ�Ԫ����cf�н��в�ѯ�����������С
				temp_startTime = System.currentTimeMillis();
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//���ļ��ж�ȡcf
				int cardinality = Utils.cuckoo_query_cardinality(false, "client/server_cipher.txt",filter);
				endTime = System.currentTimeMillis();
				System.out.println("��6���ͻ��˶�ȡCF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Client���������СΪ:" + cardinality + "\n");
			} else {
				network.receiveFile("client/client_bloom");//��cf���뱾��
				System.out.println("5.Client��������Server��BF�������뱾��client_bloom");
				
				endTime = System.currentTimeMillis();
				System.out.println("��5���ͻ��˽���BF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//7.��server_cipher.txt�е��ļ�Ԫ����BF�н��в�ѯ�����������С
				temp_startTime = System.currentTimeMillis();
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//���ļ��ж�ȡbf
				int cardinality = Utils.bloom_query_cardinality("client/server_cipher.txt",filter);
				endTime = System.currentTimeMillis();
				System.out.println("��6���ͻ��˶�ȡBF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Client���������СΪ:" + cardinality + "\n");
			}
			System.out.println("�ͻ���ִ��Rev-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�ͻ���ִ��Rev-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
			network.disconnect();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void ECC_PSI_Rev(Network network) {//�����ƽ��PSI-CAЭ��
		try {
			//1.��ʼ����Բ���߼���Կ
			eccEnc = new EccEnc();
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			if(Params.pirFilter) {// ʹ�ù�����
				//2.����Ԥ����ǰ׺��Ԫ��
				Pir_Filter(network, eccEnc);
			} else if (Params.preFilter) {
				Recv_Pre_Filter(network, eccEnc);
			} else {
				//2.��ʼ�Ա��ؼ����е�Ԫ�ؽ��м��ܣ���д���µ��ļ�client_cipher��
				Utils.ECC_enc_dec_and_Write_mThreads(eccEnc, 1, true, DB_NAME, "client/client_cipher.txt", Params.THREADS);
				endTime = System.currentTimeMillis();
				System.out.println("��2���ͻ��˼���Ԫ�غ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				System.out.println("2.Client�Ա��ؼ���Ԫ����ɼ��ܣ���д��client_cipher��");
				//3.client�����ķ��͸�server
				new Thread(() -> {
					long local_startTime = System.currentTimeMillis();
					network.sendFile("client/client_cipher.txt");//�����ļ�
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3���ͻ��˷��ͼ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					System.out.println("3.Client�ѽ�client_cipher.txt���͸�server");
				}).start();
			}
			
			//4.�������������ļ������˹���
			CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("client/recv_server_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("��4�����շ�������ĺ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdlCountDownLatch.countDown();
				System.out.println("4.Client��������Server�����ģ������뱾��recv_server_cipher.txt");
			}).start();
			
			//��֤�����ļ�������ܿ�ʼ����Ķ��μ���
			try {
				cdlCountDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			online_startTime = System.currentTimeMillis();//���Ͻ׶ο�ʼ
			//5.�������ļ����μ��ܱ���������
			//���̼߳���
			System.out.println("5.Client������Server�����Ľ��ж��μ��ܣ������뱾��server_cipher.txt");
			temp_startTime = System.currentTimeMillis();
			Utils.ECC_sec_enc_and_Write_mThreads(eccEnc, 1, true, "client/recv_server_cipher.txt", "client/server_cipher.txt", Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("��5���ͻ��˶��μ���Server����Ԫ�غ�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//6.��������Ԫ�ض��μ��ܺ󣬷��벼������������ַ��˻���
			temp_startTime = System.currentTimeMillis();
			
			if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {
				network.receiveFile("client/client_cuckoo");//��cf���뱾��
				System.out.println("6.Client��������Server��CF�������뱾��client_cuckoo");
				
				endTime = System.currentTimeMillis();
				System.out.println("��6���ͻ��˽���CF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//7.��server_cipher.txt�е��ļ�Ԫ����cf�н��в�ѯ�����������С
				temp_startTime = System.currentTimeMillis();
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//���ļ��ж�ȡcf
				int cardinality = Utils.cuckoo_query_cardinality(true, "client/server_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("��7���ͻ��˶�ȡCF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client���������СΪ:" + cardinality + "\n");
			} else {
				network.receiveFile("client/client_bloom");//��cf���뱾��
				System.out.println("6.Client��������Server��BF�������뱾��client_bloom");
				
				endTime = System.currentTimeMillis();
				System.out.println("��6���ͻ��˽���BF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
				//7.��server_cipher.txt�е��ļ�Ԫ����BF�н��в�ѯ�����������С
				temp_startTime = System.currentTimeMillis();
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//���ļ��ж�ȡbf
				int cardinality = Utils.bloom_query_cardinality("client/server_cipher.txt",filter);
				endTime = System.currentTimeMillis();
				System.out.println("��7���ͻ��˶�ȡBF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client���������СΪ:" + cardinality + "\n");
			}
			System.out.println("�ͻ���ִ��ECC-Rev-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�ͻ���ִ��ECC-Rev-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
			network.disconnect();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void PSI_Unbalanced(Network network) {//��ƽ��PSI
		try {
			network.is = network.sock.getInputStream();//�õ�Server���ݵ��ֽ���
			//���ֽ���ת��Ϊ�ַ���
			//1.�õ�����P����ʼ����ԿKey
			byte[] read = network.readBytes();
			key = new Keys(Utils.bytesToBigInteger(read, 0, read.length));
			System.out.println("1.Client�õ�����P��" + key.getP().toString());
			CommEnc.generate_Keys(key);//��ʼ��������Կ�ͽ�����Կ
			
			//2.��ʼ�Ա��ؼ����е�Ԫ�ؽ��м��ܣ���д���µ��ļ�client_cipher��
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			Utils.enc_dec_and_Write_mThreads(1, true, DB_NAME, "client/client_cipher.txt", key, Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("��2���ͻ��˼���Ԫ�غ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("2.Client�Ա��ؼ���Ԫ����ɼ��ܣ���д��client_cipher��");
			
			
			//4.��������Ԫ�ؼ��ܺ󣬷��벼¡�����������˹���
			CountDownLatch cdl = new CountDownLatch(1);
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_bloom");
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3���ͻ��˽���BF��ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("3.Client��������Server��BF�������뱾��client_bloom");
				}).start();
			} else {
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_cuckoo");
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3���ͻ��˽���CF��ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("3.Client��������Server��CF�������뱾��client_cuckoo");
				}).start();
			}
			
			//�߳����������countDownLatch����
			try {
				cdl.await();//��Ҫ�����쳣���������߳���Ϊ0ʱ����Ż��������
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//3.client�����ķ��͸�server
			new Thread(() ->  {//����
				long local_startTime = System.currentTimeMillis();
				network.sendFile("client/client_cipher.txt");//�����ļ�
				long local_endTime = System.currentTimeMillis();
				System.out.println("��4���ͻ��˷�������Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				System.out.println("4.Client��client_cipher���͸�server");
			}).start();
			
			online_startTime = System.currentTimeMillis();
			//5.�ӷ���˽����Լ����μ��ܺ��Ԫ�ؼ���
			temp_startTime = System.currentTimeMillis();
			network.receiveFile("client/recv_client_cipher_2.txt");
			endTime = System.currentTimeMillis();
			System.out.println("��5���ͻ��˴ӷ���˽��ն��μ��ܺ��Ԫ�غ�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//6.�Խ��յ������Ľ���ȥä
			temp_startTime = System.currentTimeMillis();
			Utils.enc_dec_and_Write_mThreads(1, false, "client/recv_client_cipher_2.txt", "client/recv_client_decrypt_cipher.txt", key, Params.THREADS);	
			endTime = System.currentTimeMillis();
			System.out.println("��6���ͻ��˶�����Ԫ��ȥä��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//7.��ѯBF=>�õ�������С
			temp_startTime = System.currentTimeMillis();
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//���ļ��ж�ȡbf
				int cardinality = Utils.bloom_query_cardinality("client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("��7���ͻ��˲�ѯBF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client���������СΪ:" + cardinality + "\n");
			} else {
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//���ļ��ж�ȡbf
				int cardinality = Utils.cuckoo_query_cardinality(false, "client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("��7���ͻ��˲�ѯCF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client���������СΪ:" + cardinality + "\n");
			}
			System.out.println("�ͻ���ִ��Unbalanced-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�ͻ���ִ��Unbalanced-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
	}
	
	private void ECC_PSI_Unbalanced(Network network) {//����ECC�ķ����ƽ��PSI-CAЭ��
		try {
			//1.��ʼ����Բ���߼���Կ
			eccEnc = new EccEnc();
			//2.��ʼ�Ա��ؼ����е�Ԫ�ؽ��м��ܣ���д���µ��ļ�client_cipher��
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			Utils.ECC_enc_dec_and_Write_mThreads(eccEnc, 1, true, DB_NAME, "client/client_cipher.txt", Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("��2���ͻ��˼���Ԫ�غ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("2.Client�Ա��ؼ���Ԫ����ɼ��ܣ���д��client_cipher��");
			
			//3.��������Ԫ�ؼ��ܺ󣬷��벼¡�����������˹���
			CountDownLatch cdl = new CountDownLatch(1);
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_bloom");
					long local_endTime = System.currentTimeMillis();
					System.out.println("��4���ͻ��˽���BF��ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("4.Client��������Server��BF�������뱾��client_bloom");
				}).start();
			} else {
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("client/client_cuckoo");
					long local_endTime = System.currentTimeMillis();
					System.out.println("��4���ͻ��˽���CF��ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdl.countDown();
					System.out.println("4.Client��������Server��CF�������뱾��client_cuckoo");
				}).start();
			}
			
			//�߳����������countDownLatch����
			try {
				cdl.await();//��Ҫ�����쳣���������߳���Ϊ0ʱ����Ż��������
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//4.client�����ķ��͸�server
			new Thread(() ->  {//����
				long local_startTime = System.currentTimeMillis();
				network.sendFile("client/client_cipher.txt");//�����ļ�
				long local_endTime = System.currentTimeMillis();
				System.out.println("��3���ͻ��˷�������Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				System.out.println("3.Client��client_cipher���͸�server");
			}).start();
			
			online_startTime = System.currentTimeMillis();
			//5.�ӷ���˽����Լ����μ��ܺ��Ԫ�ؼ���
			temp_startTime = System.currentTimeMillis();
			network.receiveFile("client/recv_client_cipher_2.txt");
			endTime = System.currentTimeMillis();
			System.out.println("��5���ͻ��˴ӷ���˽��ն��μ��ܺ��Ԫ�غ�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//6.�Խ��յ������Ľ���ȥä
			temp_startTime = System.currentTimeMillis();
			Utils.ECC_sec_enc_and_Write_mThreads(eccEnc, 1, false, "client/recv_client_cipher_2.txt", "client/recv_client_decrypt_cipher.txt", Params.THREADS);	
			endTime = System.currentTimeMillis();
			System.out.println("��6���ͻ��˶�����Ԫ��ȥä��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			
			//7.��ѯBF=>�õ�������С
			temp_startTime = System.currentTimeMillis();
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				BloomFilter<String> filter = Utils.BloomReader("client/client_bloom");//���ļ��ж�ȡbf
				int cardinality = Utils.bloom_query_cardinality("client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("��7���ͻ��˲�ѯBF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client���������СΪ:" + cardinality + "\n");
			} else {
				CuckooFilter<byte[]> filter = Utils.cuckooReader("client/client_cuckoo");//���ļ��ж�ȡbf
				int cardinality = Utils.cuckoo_query_cardinality(true, "client/recv_client_decrypt_cipher.txt", filter);
				endTime = System.currentTimeMillis();
				System.out.println("��7���ͻ��˲�ѯCF�󽻼���С��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("7.Client���������СΪ:" + cardinality + "\n");
			}
			
			System.out.println("�ͻ���ִ��Unbalanced-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�ͻ���ִ��Unbalanced-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}