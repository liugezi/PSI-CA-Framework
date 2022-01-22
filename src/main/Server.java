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
		DB.generateServerDB();//���ɷ�������ݼ�
		//1. ��Կ����
		switch (Params.protocol) {
		case Unbalanced://��ƽ��
			server.key = CommEnc.generate_Key();//��Կ�������
			if(server.key == null) {//��Կ����ʧ��
				System.out.println("��Կ����ʧ��");
				return;
			}
			break;
		case Reversed://�����ƽ��
			server.key = CommEnc.generate_Key();//��Կ�������
			if(server.key == null) {//��Կ����ʧ��
				System.out.println("��Կ����ʧ��");
				return;
			}
			break;
		default:
			break;
		}
		//2.�򿪼����˿ڣ���ģ��p���͸�Client
		server.connect();//����˳�ʼ��
		while(true) {
			server.start();
		}
	}

	//����˿���
	public void connect() {
		try {
			network.serverSock = new ServerSocket(8088);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("����˳�ʼ���ɹ�");
	}
	
	//��������
	public void start() throws ClassNotFoundException {
		try {
			network.sock = network.serverSock.accept();
			String address = network.sock.getInetAddress().getHostAddress();
			System.out.println("�ͻ���ipΪ��" + address);
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
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ����PIR��Ԥ���˷�����С����Ϊ���˷����󼯺Ͻ�ǰ׺���͸�С����һ������
	 * ���ж��Լ��Ƿ�ΪС��������Լ��ļ��ϴ�СС�ڵ���2^16����ǰ׺16bit���������2^16����log(n)+1
	 * Ŀǰ�ȹ̶�ǰ׺����Ϊ16bit���̶����øú�����Ϊ�󼯺�һ��
	 * @param network, key
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
	 * ��ͨ�Ĺ�ϣǰ׺Ԥ���ˣ��󼯺�Ϊ���˷���С���Ͻ�ǰ׺���Ϸ��͸��󼯺�һ������
	 * ���ж��Լ��Ƿ�ΪС���ϣ�Ȼ���ж�ǰ׺�����Ƿ���Ҫ���������ϴ�СС�ڵ���2^16����ǰ׺16bit���������2^16����log(n)+1
	 * Ŀǰ�ȹ̶�ǰ׺����Ϊ16���أ��̶����øú�����ΪС����һ��
	 * @param network
	 * @param enc
	 * @return
	 */
	public boolean Pre_Filter(Network network) {//���ݶԷ�Ԫ���������������Լ��Լ���Ԫ�����������������죬����Ҫ���ɵ�ǰ׺���ȣ���������Ӧ��BF
		try {
			network.d_out = new DataOutputStream(network.sock.getOutputStream());
			// network.d_out.writeBoolean(false);
			// 1.��Ԫ�ؼ���ǰ׺�����ܡ����ɶ�Ԫ��
			BloomFilter<String> prefixBloomFilter = new FilterBuilder()
					.expectedElements((int)Math.pow(2, Params.prefix_len))
					.falsePositiveProbability(0.000000001)
					.hashFunction(HashMethod.Murmur3)
					.buildBloomFilter();
			Utils.hash_prefix_enc_mThreads(Integer.valueOf(0), DB_NAME, "server/server_cipher.txt", prefixBloomFilter, Params.THREADS);
			// 2.����ǰ׺�Ĳ�¡��������client
			network.sendFile("server/server_prefix_bloom");
		} catch (IOException e) {
			e.printStackTrace();			
		}
		return true;
	}
	
	/**
	 * ����PIR��Ԥ���˷�����С���Ͻ������Դ󼯺ϵ�ǰ׺���ڱ��ؽ��й���
	 * ���մ󼯺�һ�������Ķ�Ԫ�飬�ڱ��ؽ��й���
	 * @param network
	 */
	public void Recv_Pir_Filter(Network network) {//���նԷ�������BF, �����նԷ���Ԫ��ǰ׺����.
    	try {
			// 1.���նԷ��Ķ�Ԫ�鼯��
			network.d_in = new DataInputStream(network.sock.getInputStream());
			System.out.println("�ȴ��󼯺�һ���Ķ�Ԫ�鼯��...");
			network.receiveFile("server/server_pair_set");
			// 2.�Ա���Ԫ������ǰ׺�Ĳ�¡������
			BloomFilter<String> filter = Utils.get_prefix_BF_mThreads(1, DB_NAME, Params.THREADS);
			// 3.�Զ�Ԫ�鼯�Ͻ��й��ˣ������µ����ݼ�
			Utils.filter_Pair_Set(filter, "server/server_pair_set", FILTERED_DB);
			
			System.out.println("��Ԫ�鼯�Ϲ������!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void PSI_Rev(Network network) {//�����ƽ��PSI-CAЭ��
		try {
			//1.��������p���ͻ���
			network.os = network.sock.getOutputStream();
			network.sendBI(key.getP());
			System.out.println("1.������ѷ�������P");
			
			if(Params.pirFilter) {//ʹ��PIRԤ����
				//2.Server��������Client�Ķ�Ԫ�鼯�ϲ����˴��뱾��
				Recv_Pir_Filter(network);
			} else if (Params.preFilter) {//ʹ��Ԥ����
				Pre_Filter(network);
			}
			//2.Server���Լ���Ԫ�ؼ��ܣ����뱾��
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			System.out.println("����˿�ʼ����Ԫ�أ�");
			//���̼߳���
			Utils.enc_dec_and_Write_mThreads(0, true, DB_NAME, "server/server_cipher.txt", key, Params.THREADS);
			endTime = System.currentTimeMillis();
			System.out.println("��2������˼���Ԫ�غ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("2.Server���Լ���Ԫ�ؼ��ܣ����뱾��server_cipher.txt�������͸�Client");
			
			//3.Server�����ķ��͸�client
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.sendFile("server/server_cipher.txt");//�����ļ�
				long local_endTime = System.currentTimeMillis();
				System.out.println("��3������˷��ͼ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				System.out.println("3.Server������server_cipher.txt���͸�Client");
			}).start();
			
			//4.�ȴ��ͻ��˷������� ���տͻ��˷��͵������ļ�
			if(!Params.pirFilter) {//��ʹ��Ԥ����ʱ�Żᵽ��һ��
				System.out.println("4.Server��ʼ����Client�������ļ���������recv_client_cipher.txt��");
				CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
				new Thread(() -> {
					long local_startTime = System.currentTimeMillis();
					network.receiveFile("server/recv_client_cipher.txt");
					long local_endTime = System.currentTimeMillis();
					System.out.println("��4������˽��ռ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
					cdlCountDownLatch.countDown();
					System.out.println("4.Server���յ�Client���ļ���������recv_client_cipher.txt��");
				}).start();
				try {
					cdlCountDownLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//5.�Կͻ��˵������ļ����ж��μ��ܣ��������Ĵ��벼�����������
			//��3������Ϊ�߳���������4���̲߳������ܲ�����CF
			online_startTime = System.currentTimeMillis();//���Ͻ׶ο�ʼ
			temp_startTime = System.currentTimeMillis();
			
			if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {//����ǲ����������
				// ʹ���벻ʹ��Ԥ���˵������ļ���ͬ
				CuckooFilter<byte[]> filter;
				if(Params.pirFilter) {
					filter = Utils.sec_encrypt_and_CuckooWriter(0, FILTERED_DB, key, Params.THREADS);
				} else {
					filter = Utils.sec_encrypt_and_CuckooWriter(0, "server/recv_client_cipher.txt", key, Params.THREADS);
				}
				
				Utils.FilterWriter(filter, "server/server_cuckoo");
				endTime = System.currentTimeMillis();
				System.out.println("��5������˼���Ԫ�ز�����CF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("5.Server���յ������Ľ��ж��μ��ܣ���д��filter��");
				
				//6.�����������������ϣ����ظ��ͻ���
				temp_startTime = System.currentTimeMillis();
				network.sendFile("server/server_cuckoo");//�����ļ�
				endTime = System.currentTimeMillis();
				System.out.println("��6������˷���CF���ͻ��˺�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Server�����������������ϣ�����CF���ظ��ͻ���");
			
			} else {//����ǲ�¡������
				BloomFilter<String> bf;
				if(Params.pirFilter) {
					bf = Utils.encrypt_and_BloomWriter(0, FILTERED_DB, key, Params.THREADS);
				} else {
					bf = Utils.encrypt_and_BloomWriter(0, "server/recv_client_cipher.txt", key, Params.THREADS);
				}
				Utils.FilterWriter(bf, "server/server_bloom");
				endTime = System.currentTimeMillis();
				System.out.println("��5������˼���Ԫ�ز�����BF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("5.Server���յ������Ľ��ж��μ��ܣ���д��filter��");
				
				//6.��¡������������ϣ����ظ��ͻ���
				temp_startTime = System.currentTimeMillis();
				network.sendFile("server/server_bloom");//�����ļ�
				endTime = System.currentTimeMillis();
				System.out.println("��6������˷���BF���ͻ��˺�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
				System.out.println("6.Server��¡������������ϣ�����BF���ظ��ͻ���");
			}
			
			System.out.println("�����ִ��rev-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�����ִ��rev-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void ECC_PSI_Rev(Network network) {//�����ƽ��PSI-CAЭ��
		long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
		long online_startTime;
		long endTime;
		long temp_startTime;
		//1.��ʼ����Բ���߼���Կ
		eccEnc = new EccEnc();
		
		if(Params.pirFilter) {//ʹ��Ԥ����
			//Server��������Client�Ķ�Ԫ�鼯�ϲ����˴��뱾��
			Recv_Pir_Filter(network);
		} else if (Params.preFilter) {//ʹ��Ԥ����
			Pre_Filter(network);
		}
		
		//2.����˽��Լ���Ԫ�ؼ��ܣ����뱾��
		try {
			Utils.ECC_enc_dec_and_Write_mThreads(eccEnc, 0, true, DB_NAME, "server/server_cipher.txt", Params.THREADS);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		endTime = System.currentTimeMillis();
		System.out.println("��2������˼���Ԫ�غ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
		System.out.println("2.Server���Լ���Ԫ�ؼ��ܣ����뱾��server_cipher.txt�������͸�Client");
		
		online_startTime = System.currentTimeMillis();//���Ͻ׶ο�ʼ
		if(!Params.pirFilter) {
			//3.�ȴ��ͻ��˷������� ���տͻ��˷��͵������ļ�
			System.out.println("3.Server��ʼ����Client�������ļ���������recv_client_cipher.txt��");
			CountDownLatch cdlCountDownLatch = new CountDownLatch(1);
			new Thread(() -> {
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("server/recv_client_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("��3������˽��ռ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdlCountDownLatch.countDown();
				System.out.println("3.Server���յ�Client���ļ���������recv_client_cipher.txt��");
			}).start();
			try {
				cdlCountDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//4.Server�����ķ��͸�client
		new Thread(() -> {
			long local_startTime = System.currentTimeMillis();
			network.sendFile("server/server_cipher.txt");//�����ļ�
			long local_endTime = System.currentTimeMillis();
			System.out.println("��3������˷��ͼ���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
			System.out.println("3.Server������server_cipher.txt���͸�Client");
		}).start();
		
		//5.�Կͻ��˵������ļ����ж��μ��ܣ��������Ĵ��벼�����������
		//��3������Ϊ�߳���������4���̲߳������ܲ�����CF
		temp_startTime = System.currentTimeMillis();
		if(Params.filterEnum == Params.FilterEnum.CuckooFilter) {//����ǲ����������
			System.out.println("����˿�ʼ����Ԫ�ز���CF");
			CuckooFilter<byte[]> filter;
			if(Params.pirFilter) {
				filter = Utils.sec_encrypt_and_CuckooWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
			} else {
				filter = Utils.sec_encrypt_and_CuckooWriter(0, "server/recv_client_cipher.txt", eccEnc, Params.THREADS);
			}
			
			Utils.FilterWriter(filter, "server/server_cuckoo");
			endTime = System.currentTimeMillis();
			System.out.println("��5������˼���Ԫ�ز�����CF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("5.Server���յ������Ľ��ж��μ��ܣ���д��filter��");
		
			//6.�����������������ϣ����ظ��ͻ���
			temp_startTime = System.currentTimeMillis();
			network.sendFile("server/server_cuckoo");//�����ļ�
			endTime = System.currentTimeMillis();
			System.out.println("��6������˷���CF���ͻ��˺�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("6.Server�����������������ϣ�����CF���ظ��ͻ���");
		} else {//����ǲ�¡������
			System.out.println("����˿�ʼ����Ԫ�ز���BF");
			BloomFilter<String> bf; 
			if(Params.pirFilter) {
				bf = Utils.sec_encrypt_and_BloomWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
			} else {
				bf = Utils.sec_encrypt_and_BloomWriter(0, "server/recv_client_cipher.txt", eccEnc, Params.THREADS);
			}
			
			Utils.FilterWriter(bf, "server/server_bloom");
			endTime = System.currentTimeMillis();
			System.out.println("��5������˼���Ԫ�ز�����BF��ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("5.Server���յ������Ľ��ж��μ��ܣ���д��filter��");
			
			//6.��¡������������ϣ����ظ��ͻ���
			temp_startTime = System.currentTimeMillis();
			network.sendFile("server/server_bloom");//�����ļ�
			endTime = System.currentTimeMillis();
			System.out.println("��6������˷���BF���ͻ��˺�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("6.Server��¡������������ϣ�����BF���ظ��ͻ���");
		}
		
		System.out.println("�����ִ��ECC-rev-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
		System.out.println("�����ִ��ECC-rev-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		
		//network.disconnect();
	}
	
	private void PSI_Unbalanced(Network network) {//��ͨ��ƽ��PSI-CAЭ��
		try {
			//1.��������p���ͻ���
			network.os = network.sock.getOutputStream();
			network.sendBI(key.getP());
			System.out.println("1.������ѷ�������P");
		
			//2.�����Լ���Ԫ�أ������벼¡������
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {//ʹ�ò�¡������
				
				BloomFilter<String> bf;
//				if(isFiltered) {
//					bf = Utils.encrypt_and_BloomWriter(0, FILTERED_DB, key, Params.THREADS);
//				} else {
//					
//				}
				bf = Utils.encrypt_and_BloomWriter(0, DB_NAME, key, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("��2������˼���Ԫ�ز�����BF��ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				online_startTime = System.currentTimeMillis();
				//3.����¡���������͸��ͻ��ˣ�ͬʱ�ȴ����տͻ��˷����ļ���Ԫ�أ������½������߳�ʵ��ȫ˫��ͨ��
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(bf, "server/server_bloom");
					network.sendFile("server/server_bloom");//�����ļ�
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3������˽�BF���͸��ͻ��˺�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
				
			} else {//ʹ�ò����������
				CuckooFilter<byte[]> filter;
//				if(isFiltered) {
//					filter = Utils.encrypt_and_CuckooWriter(0, FILTERED_DB, key, Params.THREADS);
//				} else {
//					
//				}
				filter = Utils.encrypt_and_CuckooWriter(0, DB_NAME, key, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("��2������˼���Ԫ�ز�����CF��ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				online_startTime = System.currentTimeMillis();
				//3.����������������͸��ͻ��ˣ�ͬʱ�ȴ����տͻ��˷����ļ���Ԫ�أ������½������߳�ʵ��ȫ˫��ͨ��
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(filter, "server/server_cuckoo");
					network.sendFile("server/server_cuckoo");//�����ļ�
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3������˽�CF���͸��ͻ��˺�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
			}
			
			final CountDownLatch cdl = new CountDownLatch(1);//����Ϊ�̸߳���
			new Thread(() ->  {//����
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("server/recv_client_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("��4������˽��տͻ��˼���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdl.countDown();//�����̱߳���ȫ��ִ������ٿ�ʼ���μ���
			}).start();
			//�߳����������countDownLatch����
			try {
				cdl.await();//��Ҫ�����쳣���������߳���Ϊ0ʱ����Ż��������
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//4.���տͻ��˵ļ���Ԫ�غ󣬶�����ж��μ��ܣ�������Ϻ󷵻ظ��ͻ���
			temp_startTime = System.currentTimeMillis();
			Utils.enc_dec_and_Write_mThreads(0, true, "server/recv_client_cipher.txt", "server/recv_client_cipher2.txt", key, Params.THREADS);
			network.sendFile("server/recv_client_cipher2.txt");//�����ļ�
			endTime = System.currentTimeMillis();
			System.out.println("��4������˶��μ���Ԫ�ز����ؿͻ��˺�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("�����ִ��Unbalanced-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�����ִ��Unbalanced-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void ECC_PSI_Unbalanced(Network network) {//��ƽ��PSI-CAЭ��
		try {
			long startTime = System.currentTimeMillis(); //��ȡ��ʼʱ��
			long online_startTime;
			long endTime;
			long temp_startTime;
			//1.��ʼ����Բ���߼���Կ
			eccEnc = new EccEnc();
		
			//2.�����Լ���Ԫ�أ������벼¡������
			if(Params.filterEnum == Params.FilterEnum.BloomFilter) {
				BloomFilter<String> bf;
//				if(isFiltered) {
//					bf = Utils.encrypt_and_BloomWriter(0, FILTERED_DB, eccEnc, Params.THREADS);
//				} else {
//					
//				}
				bf = Utils.encrypt_and_BloomWriter(0, DB_NAME, eccEnc, Params.THREADS);
				
				endTime = System.currentTimeMillis();
				System.out.println("��2������˼���Ԫ�ز�����BF��ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				//3.����¡���������͸��ͻ��ˣ�ͬʱ�ȴ����տͻ��˷����ļ���Ԫ�أ������½������߳�ʵ��ȫ˫��ͨ��
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(bf, "server/server_bloom");
					network.sendFile("server/server_bloom");//�����ļ�
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3������˽�BF���͸��ͻ��˺�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
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
				System.out.println("��2������˼���Ԫ�ز�����CF��ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
				
				//3.����������������͸��ͻ��ˣ�ͬʱ�ȴ����տͻ��˷����ļ���Ԫ�أ������½������߳�ʵ��ȫ˫��ͨ��
				new Thread(() ->  {//����
					long local_startTime = System.currentTimeMillis();
					Utils.FilterWriter(filter, "server/server_cuckoo");
					network.sendFile("server/server_cuckoo");//�����ļ�
					long local_endTime = System.currentTimeMillis();
					System.out.println("��3������˽�CF���͸��ͻ��˺�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				}).start();
			}
			
			final CountDownLatch cdl = new CountDownLatch(1);//����Ϊ�̸߳���
			new Thread(() ->  {//����
				long local_startTime = System.currentTimeMillis();
				network.receiveFile("server/recv_client_cipher.txt");
				long local_endTime = System.currentTimeMillis();
				System.out.println("��4������˽��տͻ��˼���Ԫ�غ�ʱ:" + (local_endTime - local_startTime) / 1000 + "s" + (local_endTime - local_startTime) % 1000 + "ms");
				cdl.countDown();//�����̱߳���ȫ��ִ������ٿ�ʼ���μ���
			}).start();
			//�߳����������countDownLatch����
			try {
				cdl.await();//��Ҫ�����쳣���������߳���Ϊ0ʱ����Ż��������
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			online_startTime = System.currentTimeMillis();
			
			//4.���տͻ��˵ļ���Ԫ�غ󣬶�����ж��μ��ܣ�������Ϻ󷵻ظ��ͻ���
			temp_startTime = System.currentTimeMillis();
			Utils.ECC_sec_enc_and_Write_mThreads(eccEnc, 0, true, "server/recv_client_cipher.txt", "server/recv_client_cipher2.txt", Params.THREADS);
			network.sendFile("server/recv_client_cipher2.txt");//�����ļ�
			endTime = System.currentTimeMillis();
			System.out.println("��4������˶��μ���Ԫ�ز����ؿͻ��˺�ʱ:" + (endTime - temp_startTime) / 1000 + "s" + (endTime - temp_startTime) % 1000 + "ms");
			System.out.println("�����ִ��Unbalanced-PSI-CA�ܺ�ʱ:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
			System.out.println("�����ִ��Unbalanced-PSI-CA���Ͻ׶��ܺ�ʱ:" + (endTime - online_startTime) / 1000 + "s" + (endTime - online_startTime) % 1000 + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


//public void Recv_Pre_Filter(Network network) {//���նԷ�������BF, �����նԷ���Ԫ��ǰ׺����.
//try {
//	//�Ȼ�ȡ�Լ���Ԫ������
//	long nums = Params.server_size;
//	network.d_out = new DataOutputStream(network.sock.getOutputStream());
//	//��Ԫ���������͸��Է�
//	network.d_out.writeLong(nums);
//	
//	//���նԷ��Ĺ�������
//	network.d_in = new DataInputStream(network.sock.getInputStream());
//	boolean isOK = network.d_in.readBoolean();
//	if(!isOK) {
//		System.out.println("�Է��ܾ����ݹ�������,��ʼ������PSI-CA����");
//		isFiltered = false;
//		return;
//	}
//	
//	//���նԷ���ǰ׺����
//	isFiltered = true;
//	System.out.println("�Է�ͬ�����ݹ�������,��ʼ����");
//	network.d_in = new DataInputStream(network.sock.getInputStream());
//	int pre_len = network.d_in.readInt();
//	
//	//���նԷ���BF
//	network.receiveFile("client/server_pre_bloom");
//	
//	BloomFilter<String> filter = new FilterBuilder()
//			.expectedElements(Params.client_size)
//			.falsePositiveProbability(0.0000000001)
//			.hashFunction(HashMethod.Murmur3)
//			.buildBloomFilter();
//	// BloomFilter<String> filter = Utils.BloomReader("client/server_pre_bloom");
//	
//	//��ʼ��ȡ�����ļ��е�Ԫ�أ����й��ˣ������µ����ݼ�
//	FileReader fileReader = new FileReader(DB_NAME);
//	BufferedReader bReader = new BufferedReader(fileReader);
//	FileWriter fileWriter = new FileWriter(FILTERED_DB_NAME);
//	BufferedWriter bWriter = new BufferedWriter(fileWriter);
//	
//	int cnt = 0;
//	String lineString = "";
//	byte[] result = null;
//	while((lineString = bReader.readLine()) != null) {
//		result = PreReduce.Hash_and_Get_Bits(lineString, pre_len);//��ϣ����ȡǰ׺
//		
//		if(filter.contains(result)) {//���Ԫ��ǰ׺��ƥ����, ��ֱ��д�����ļ���
//			bWriter.write(lineString + "\r\n");
//			cnt++;
//		}
//	}
//	System.out.println("ǰ׺ƥ���ϵ�Ԫ������:" + cnt);
//	bReader.close();
//	bWriter.close();
//} catch (IOException e) {
//	e.printStackTrace();
//}
//}

//public boolean Pre_Filter(Network network) {
//try {
//	network.d_in = new DataInputStream(network.sock.getInputStream());
//	long recv_nums = network.d_in.readLong();//�ļ��ܳ���
//	//long order_gap = recv_nums / Params.client_size;//��ʱ�Է���������Ӧ�ô����Լ�Ϊ��
//	
//	int log_size_other = (int) PreReduce.log2(recv_nums);//�Է�����������������ȡ��
//	int log_size_local = (int) Math.ceil(PreReduce.log2(Params.client_size));//�Լ�������������ȡ��
//	
//	int diff = log_size_other - log_size_local;
//	
//	if(diff < 7) {
//		//���������ˣ�����false
//		network.d_out = new DataOutputStream(network.sock.getOutputStream());
//		network.d_out.writeBoolean(false);
//		isFiltered = false;
//		System.out.println("���������С�ڵ���2^7, �����Թ���");
//		return false;//���������������С��2^7, ����Ϊ���˲�������ȫ, �Լ������ݴ��ڱ�¶����, Ӧ�ܾ�ǰ׺����
//	} else {
//		//�������ˣ�����true
//		network.d_out = new DataOutputStream(network.sock.getOutputStream());
//		network.d_out.writeBoolean(true);
//		isFiltered = true;
//		//�����������������ڵ���2^7, ����Լ�������
//		System.out.println("�����������ڵ���2^7, ���Թ���");
//		int pre_len = log_size_local + diff - 7;//ǰ׺�����Զ����룬��֤log_size_other-pre_len = 7
//		//��ʼ����
//		FileReader fileReader = new FileReader(DB_NAME);
//		BufferedReader bReader = new BufferedReader(fileReader);
//		BloomFilter<String> filter = new FilterBuilder()
//				.expectedElements(Params.client_size)
//				.falsePositiveProbability(0.0000000001)
//				.hashFunction(HashMethod.Murmur3)
//				.buildBloomFilter();
//		String lineString = "";
//		byte[] result;
//		//��ȡ�ļ�����ȡǰ׺, Ȼ����벼¡������
//		while ((lineString = bReader.readLine()) != null) {//��ȡ�ļ�
//			result = PreReduce.Hash_and_Get_Bits(lineString, pre_len);//��ϣ����ȡǰ׺
//			filter.add(result);//����BF
//		}
//		bReader.close();
//		//�����BF���͸������
//		//�Ƚ�ǰ׺���ȷ���ȥ
//		network.d_out.writeInt(pre_len);
//		//�ٽ�BF����ȥ
//		Utils.FilterWriter(filter, "client/pre_bloom");
//		network.sendFile("client/pre_bloom");
//	}
//} catch (IOException e) {
//	e.printStackTrace();			
//}
//return true;
//}