import static org.junit.Assert.assertNotNull;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.spec.ECPoint;
import java.util.concurrent.CountDownLatch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.JSONWriter;
import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;
import com.github.mgunlogson.cuckoofilter4j.Utils.Algorithm;
import com.google.common.hash.Funnels;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider.HashMethod;

public class Utils {//工具类方法
	
	private static BufferedOutputStream out = null;
	
	public static BigInteger bytesToBigInteger(byte[] in, int inOff, int inLen) {//将bytes数组转为BigInteger
        byte[]  block;
        
        if (inOff != 0 || inLen != in.length) {
            block = new byte[inLen];
            System.arraycopy(in, inOff, block, 0, inLen);
        } else {
            block = in;
        }
        
        BigInteger res = new BigInteger(1, block);//1表示符号为正
        
        return res;
    }

    public static byte[] bigIntegerToBytes(BigInteger input) {//将BigInteger转为byte数组
        byte[] output = input.toByteArray();
        if (output[0] == 0) {//正数BigInteger,会有符号位,去除第一个符号位0,还原得到原始数组
            byte[] tmp = new byte[output.length - 1];
            System.arraycopy(output, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return output;
    }
    
    public static String bytesToHexString(byte[] src) {//byte[]数组转16进制字符串
    	StringBuilder stringBuilder = new StringBuilder("");
    	if(src == null || src.length <= 0) {
    		return null;
    	}
    	for(int i = 0; i < src.length; i++) {
    		int v = src[i] & 0xFF;
    		String hv = Integer.toHexString(v);
    		if(hv.length() < 2) {
    			stringBuilder.append(0);
    		}
    		stringBuilder.append(hv);
    	}
    	return stringBuilder.toString();
    }
    
    public static byte[] hexStringToBytes(String hexString) {//16进制字符串转为字节
    	if(hexString == null || hexString.equals("")) {
    		return null;
    	}
    	hexString = hexString.toUpperCase();
    	int length = hexString.length() / 2;
    	char[] hexChars = hexString.toCharArray();
    	byte[] d = new byte[length];
    	for(int i = 0; i < length; i++) {
    		int pos = i * 2;
    		d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
    	}
    	return d;
    }
    
    private static byte charToByte(char c) {
    	return (byte) "0123456789ABCDEF".indexOf(c);
    }
    
    public static void writeLineToFile(File file, byte[] line, int begin, int end) {//将byte[]按行写入文件
		if (begin == 0) {
			try {
				out = new BufferedOutputStream(new FileOutputStream(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		try {			
			out.write(line);	
		} catch (IOException e) {
			e.printStackTrace();
		}	
		if (begin == end - 1) {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
    
    public static int getLineNumber(File file) throws IOException {
    	if(file.exists()) {
			try {
				FileReader fileReader = new FileReader(file);
				LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
				lineNumberReader.skip(Long.MAX_VALUE);
				int lines = lineNumberReader.getLineNumber();
				fileReader.close();
				lineNumberReader.close();
				return lines;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
    	}
    	return 0;
    }
    
    public static void FilterWriter(CuckooFilter<byte[]> filter, String filePath) {//将布谷鸟过滤器写入文件的方法
    	File file = new File(filePath);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(filter);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void FilterWriter(BloomFilter<String> filter, String filePath) {
        ObjectOutputStream out = null;
        try {
        	File file = new File(filePath);
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(filter);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static CuckooFilter<byte[]> cuckooReader(String filePath) {//从文件中读取布谷鸟过滤器的方法
    	File file = new File(filePath);
        FileInputStream fileIn = null;
        ObjectInputStream in = null;
        CuckooFilter<byte[]> filter = null;
        try {
            fileIn = new FileInputStream(file);
            in = new ObjectInputStream(fileIn);
            filter = (CuckooFilter<byte[]>)in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                if(in != null)
                    in.close();
                if(fileIn != null)
                    fileIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filter;
    }
    
    public static BloomFilter<String> BloomReader(String filePath) {//从文件中读取布隆过滤器的方法
        FileInputStream fileIn = null;
        ObjectInputStream in = null;
        BloomFilter<String> filter = null;
        try {
        	File file = new File(filePath);
            fileIn = new FileInputStream(file);
            in = new ObjectInputStream(fileIn);
            filter = (BloomFilter<String>)in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                if(in != null)
                    in.close();
                if(fileIn != null)
                    fileIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filter;
    }
    
    //（(单线程）从from_path中读取密文，用key加密，将加密后的密文写入to_path
    public static void encrypt_and_Write(String from_path, String to_path, Keys key) {
		try {
			FileReader fileReader;
			File file = new File(from_path);
			fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
		    String line = "";
		    FileWriter cipher_writer = new FileWriter(to_path, true);//存储密文的目标文件
		    long startTime = System.currentTimeMillis();
		    int i = 0;
	        while ((line = bufferedReader.readLine()) != null) {
				//加密后将密文转为byte[]，并放入CF中，一步到位
	        	
	        	cipher_writer.write(CommEnc.encrypt_BigInteger(new BigInteger(line), key).toString().getBytes("utf-8") + "\r\n");
				if(i++ % 5000 == 0) {
					//System.out.println("第" + i + "个密文已经加密完毕并存入" + to_path + "中");
					long endTime = System.currentTimeMillis();
					System.out.println("加密" + i + "个元素的程序运行时间： "+(endTime-startTime)/1000 + "s" +
							 (endTime-startTime) % 1000 +"ms");
				}
		    }
	        cipher_writer.close();//关闭读写接口
	        bufferedReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 拆分文件成Thread份
     * @param pathString 文件路径
     * @param stage 当前处于的阶段，用于输出文件名
     * @param file 文件
     * @param len 文件长度
     * @param threads 线程数
     */
    public static void split_File(String pathString, String stage, File file, int len, int threads) {
    	try {
			String contentLine = "";
			System.out.println("要加密的元素个数" + len);
			int NumPerThread = len / threads;//每个线程需要处理的密文数量
			int cnt = 0;//用来数已经读取了多少行密文
			FileReader fileReader = new FileReader(file);
			BufferedReader bReader = new BufferedReader(fileReader);
			FileWriter fileWriter;
			
			for(int i = 1; i <= threads; ++i) {
				fileWriter = new FileWriter(pathString + stage + i);
				BufferedWriter bWriter = new BufferedWriter(fileWriter);
				while((contentLine = bReader.readLine()) != null) {
					bWriter.write(contentLine);
					bWriter.newLine();
					cnt++;
					if(cnt % NumPerThread == 0 && i < threads) {//线程i所需的密文读取完毕，暂时跳出while循环,但如果i=threads，那就把剩下多出来的密文都读完
						break;
					}
				}
				bWriter.close();
			}			
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    //将Thread份文件合成一份
    //输入参数: 文件路径、toPath、Thread
    public static void join_Files(String pathString, String toPath, int threads) {
    	FileWriter fileWriter;
    	String temp_line = null;
		try {
			fileWriter = new FileWriter(toPath);
			FileReader fileReader = null;
			BufferedReader bReader = null;
			BufferedWriter bWriter = new BufferedWriter(fileWriter);
			for(int i = 1; i <= threads; ++i) {
				fileReader = new FileReader(pathString + i + "out");
				bReader = new BufferedReader(fileReader);
				//String temp_line = null;
				while((temp_line = bReader.readLine()) != null) {//逐行读取元素并加密
					//加密后的内容写入outj文件中
					bWriter.write(temp_line + "\r\n");
				}
			}	
			bReader.close();
			bWriter.close();
			fileReader.close();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 多线程哈希再加密元素的方法
     * @param role
     * @param from_path
     * @param to_path
     * @param enc Object类型，根据传入的对象决定使用Pohlig-Hellman还是ECC
     * @param threads
     * @throws FileNotFoundException
     */
    //（多线程）从from_path中读取密文，拆分成多份子文件，多线程分别对子文件用key加密，将加密后的密文并发写入to_path
    public static void hash_prefix_enc_mThreads(Integer role, String from_path, String to_path, Object enc, int threads) throws FileNotFoundException {
		if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂次
			System.out.println("线程数必须为2的幂次！");
			return;
		}
		//提取出保存文件的根目录
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {//0代表server
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		//1.先读取文件长度
		long startTime = System.currentTimeMillis();
		int len;
		File file = new File(from_path);
		try {
			len = getLineNumber(file);
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
			split_File(pathString, Params.StageEnum.Prefix_filter.toString(), file, len, threads);//将目标文件拆分成threads份
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
		final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
		for(int j = 1; j <= threads; ++j) {
			int new_j = j;
			new Thread(() ->  {
				try {
					FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Prefix_filter.toString() + new_j);
					BufferedReader temp_bReader = new BufferedReader(temp_reader);
					FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Prefix_filter.toString() + new_j + "out");
					BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
					String temp_line = null;
					String hash_prefix = null;
					String hash_String = null;
					String s = Params.pub_random.toString();
					
					if (enc.getClass().equals(Keys.class)) {// 如果是Keys类型, 则采用Pohlig-Hellman加密
						Keys key = (Keys)enc;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							// 计算哈希前缀h(x)[0:w]
//							System.out.println("prefix_len:" + Params.prefix_len);
//							System.out.println("前缀的字节长度:" + PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len).length);
							hash_prefix = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len));
//							System.out.println("hash_prefix:" + hash_prefix);
							// 得到哈希值h(s,x)再加密, 并将哈希前缀与密文组成二元组(h(x)[0:w],h(s,x))写入文件
							hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							temp_bWriter.write(hash_prefix + "," + CommEnc.encrypt_BigInteger(new BigInteger(hash_String, 16), key).toString() + "\r\n");
						}
					} else if (enc.getClass().equals(EccEnc.class)){// 如果是椭圆曲线类型，则采用ECC加密
						org.bouncycastle.math.ec.ECPoint element;
						EccEnc eccEnc = (EccEnc)enc;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							// 得到哈希值h(s,x)再加密,写入文件
//							System.out.println("prefix_len:" + Params.prefix_len);
//							System.out.println("前缀的字节长度:" + PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len).length);
							hash_prefix = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len));
//							System.out.println("hash_prefix:" + hash_prefix);
							hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							element = eccEnc.BigIntegerToPoint(new BigInteger(hash_String, 16));
							temp_bWriter.write(hash_prefix + "," + Utils.bytesToHexString(eccEnc.encryptPoint(element).getEncoded(true)) + "\r\n");
						}
					} else {
						System.out.println("传入的加密对象错误！");
						temp_bWriter.close();
						temp_bReader.close();
						return;
					}
					temp_bWriter.close();
					temp_bReader.close();
					cdl.countDown();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "t" + j).start();
		}
		//线程启动后调用countDownLatch方法
		try {
			cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
		//3.密文加密结束后，将所有产生的密文文件合成一份
		join_Files(pathString + Params.StageEnum.Prefix_filter.toString(), to_path, threads);
    }
    
    /**
     * server多线程哈希存入布隆过滤器
     * @param role
     * @param from_path
     * @param to_path
     * @param threads
     * @throws FileNotFoundException
     */
    public static void hash_prefix_enc_mThreads(Integer role, String from_path, String to_path, BloomFilter<String> bf, int threads) throws FileNotFoundException {
		if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂次
			System.out.println("线程数必须为2的幂次！");
			return;
		}
		//提取出保存文件的根目录
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {//0代表server
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		//1.先读取文件长度
		long startTime = System.currentTimeMillis();
		int len;
		File file = new File(from_path);
		try {
			len = getLineNumber(file);
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
			split_File(pathString, Params.StageEnum.Prefix_filter.toString(), file, len, threads);//将目标文件拆分成threads份
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
		final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
		for(int j = 1; j <= threads; ++j) {
			int new_j = j;
			new Thread(() ->  {
				try {
					FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Prefix_filter.toString() + new_j);
					BufferedReader temp_bReader = new BufferedReader(temp_reader);
					FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Prefix_filter.toString() + new_j + "out");
					BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
					String temp_line = null;
					String hash_prefix = null;
					
					while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
						// 计算哈希前缀h(x)[0:w]
//						System.out.println("prefix_len:" + Params.prefix_len);
//						System.out.println("前缀的字节长度:" + PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len).length);
						hash_prefix = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len));
//							System.out.println("hash_prefix:" + hash_prefix);
						bf.add(hash_prefix);
						temp_bWriter.write(hash_prefix + "\r\n");
					}
					temp_bWriter.close();
					temp_bReader.close();
					cdl.countDown();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "t" + j).start();
		}
		//线程启动后调用countDownLatch方法
		try {
			cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
		if (role == 0) {
			Utils.FilterWriter(bf, pathString + "server_prefix_bloom");
		} else {
			Utils.FilterWriter(bf, pathString + "client_prefix_bloom");
		}
		
		//3.密文加密结束后，将所有产生的密文文件合成一份
		join_Files(pathString + Params.StageEnum.Prefix_filter.toString(), to_path, threads);
    }
    
//   
    
    /**
     * 小集合一方生成前缀值的BF集合，多线程插入
     * @param role
     * @param from_path
     * @param to_path
     * @param threads
     */
    public static BloomFilter<String> get_prefix_BF_mThreads(Integer role, String from_path, int threads) {
    	File file = new File(from_path);//存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
		    int len = getLineNumber(file);//获取文件行数，以得知元素个数
		    if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布隆过滤器
			BloomFilter<String> filter = new FilterBuilder()
					.expectedElements(len)
					.falsePositiveProbability(0.000000001)
					.hashFunction(HashMethod.Murmur3)
					.buildBloomFilter();
		    
			long startTime = System.currentTimeMillis(); //获取开始时间
			long endTime;
			int i;
			//多线程并发插入BF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Prefix_bloom.toString(), file, len, threads);
			//2. 对拆分后产生的文件进行并发访问求前缀,同时存入BF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() ->  {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Prefix_bloom.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							// 对元素求哈希前缀,插入BF,存储的是哈希值的十六进制字符串
							filter.add(Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len)));
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			endTime = System.currentTimeMillis();
			System.out.println("计算元素前缀并存入BF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 小集合一方根据事先生成好的布隆过滤器对收到的二元组集合进行过滤, 存在则留下
     * @param bf
     * @param from_path
     * @param to_path
     */
    // 也可以多线程, 但其实没必要, 快不了多少
    public static void filter_Pair_Set(BloomFilter<String> bf, String from_path, String to_path) {
		long startTime = System.currentTimeMillis();
		try {
			File file = new File(from_path);
			FileReader fileReader = new FileReader(file);
			BufferedReader temp_bReader = new BufferedReader(fileReader);
			FileWriter temp_writer = new FileWriter(to_path);
			BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
			String temp_line = null;

			while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
				// 按逗号分割, 取前半部分前缀进行查询, 如果存在, 就将后半部分写入新文件
				String[] pair = temp_line.split(",");
				if (bf.contains(pair[0])) {
					temp_bWriter.write(pair[1] + "\r\n");
				}
			}
			temp_bReader.close();
			temp_bWriter.close();
			long endTime = System.currentTimeMillis();
			System.out.println("过滤大集合元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");
		} catch (IOException e) {
			System.out.println("文件读写异常！");
			e.printStackTrace();
		}
    }
    
    /**
     * 多线程预过滤，计算元素集合前缀，对过滤器做查询，存在则加密并存入新文件中
     * @param bf
     * @param from_path
     * @param to_path
     * @param enc
     * @param threads
     */
    public static void filter_Set_mThreads(Integer role, BloomFilter<String> bf, String from_path, String to_path, Object enc, int threads) {
    	if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂次
			System.out.println("线程数必须为2的幂次！");
			return;
		}
		//提取出保存文件的根目录
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {//0代表server
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		//1.先读取文件长度
		long startTime = System.currentTimeMillis();
		int len;
		File file = new File(from_path);
		try {
			len = getLineNumber(file);
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
			split_File(pathString, Params.StageEnum.Prefix_filter.toString(), file, len, threads);//将目标文件拆分成threads份
		} catch (IOException e1) {
			e1.printStackTrace();
		}
			
		//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
		final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
		for(int j = 1; j <= threads; ++j) {
			int new_j = j;
			new Thread(() ->  {
				try {
					FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Prefix_filter.toString() + new_j);
					BufferedReader temp_bReader = new BufferedReader(temp_reader);
					FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Prefix_filter.toString() + new_j + "out");
					BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
					String temp_line = null;
					String hash_prefix = null;
					String hash_String = null;
					String s = Params.pub_random.toString();
					
					if (enc.getClass().equals(Keys.class)) {// 如果是Keys类型, 则采用Pohlig-Hellman加密
						Keys key = (Keys)enc;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							// 计算哈希前缀h(x)[0:w]
//							System.out.println("prefix_len:" + Params.prefix_len);
//							System.out.println("前缀的字节长度:" + PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len).length);
							hash_prefix = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len));
//							System.out.println("hash_prefix:" + hash_prefix);
							// 得到哈希值h(s,x)再加密, 并将哈希前缀与密文组成二元组(h(x)[0:w],h(s,x))写入文件
							if (bf.contains(hash_prefix)) {
								hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
								temp_bWriter.write(CommEnc.encrypt_BigInteger(new BigInteger(hash_String, 16), key).toString() + "\r\n");
							}
						}
					} else if (enc.getClass().equals(EccEnc.class)){// 如果是椭圆曲线类型，则采用ECC加密
						org.bouncycastle.math.ec.ECPoint element;
						EccEnc eccEnc = (EccEnc)enc;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							// 得到哈希值h(s,x)再加密,写入文件
//							System.out.println("prefix_len:" + Params.prefix_len);
//							System.out.println("前缀的字节长度:" + PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len).length);
							hash_prefix = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len));
//							System.out.println("hash_prefix:" + hash_prefix);
							if (bf.contains(hash_prefix)) {
								hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
								element = eccEnc.BigIntegerToPoint(new BigInteger(hash_String, 16));
								temp_bWriter.write(Utils.bytesToHexString(eccEnc.encryptPoint(element).getEncoded(true)) + "\r\n");
							}
						}
					} else {
						System.out.println("传入的加密对象错误！");
						temp_bWriter.close();
						temp_bReader.close();
						return;
					}
					
					temp_bWriter.close();
					temp_bReader.close();
					cdl.countDown();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "t" + j).start();
		}
		//线程启动后调用countDownLatch方法
		try {
			cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
		//3.密文加密结束后，将所有产生的密文文件合成一份
		join_Files(pathString + Params.StageEnum.Prefix_filter.toString(), to_path, threads);
    }
    
    
    //（多线程）从from_path中读取密文，拆分成多份子文件，多线程分别对子文件用key加密，将加密后的密文并发写入to_path
    // isEnc用来决定是执行加密还是解密
    public static void enc_dec_and_Write_mThreads(Integer role, Boolean isEnc, String from_path, String to_path, Keys key, int threads) throws FileNotFoundException {
		if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂
			System.out.println("线程数必须为2的幂次！");
			return;
		}
		//提取出保存文件的根目录
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {//0代表server
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		//1.先读取文件长度
		long startTime = System.currentTimeMillis();
		int len;
		File file = new File(from_path);
		try {
			len = getLineNumber(file);
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
			split_File(pathString, Params.StageEnum.Encrypt.toString(), file, len, threads);//将目标文件拆分成threads份
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
		final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
		for(int j = 1; j <= threads; ++j) {
			int new_j = j;
			new Thread(() ->  {
				try {
					FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Encrypt.toString() + new_j);
					BufferedReader temp_bReader = new BufferedReader(temp_reader);
					FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Encrypt.toString() + new_j + "out");
					BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
					String temp_line = null;
					if(isEnc) {//isEnc为true，则逐行读取并加密
						
						//String hash_String = null;
						//String s = Params.pub_random.toString();
						
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							//加密后的内容写入outj文件中
							//hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							//temp_bWriter.write(CommEnc.encrypt_BigInteger(new BigInteger(hash_String, 16), key).toString() + "\r\n");
							temp_bWriter.write(CommEnc.encrypt_BigInteger(new BigInteger(temp_line), key).toString() + "\r\n");
						}
					} else {//isEnc为false，则逐行读取并解密
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并解密
							//解密后的内容写入outj文件中
							temp_bWriter.write(CommEnc.decrypt_BigInteger(new BigInteger(temp_line), key).toString() + "\r\n");
						}
					}
					temp_bWriter.close();
					temp_bReader.close();
					cdl.countDown();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "t" + j).start();
		}
		//线程启动后调用countDownLatch方法
		try {
			cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
		//3.密文加密结束后，将所有产生的密文文件合成一份
		join_Files(pathString + Params.StageEnum.Encrypt.toString(), to_path, threads);
    }
    
    /**
     * 首次加密或解密
     * @param ecc
     * @param role
     * @param isEnc
     * @param from_path
     * @param to_path
     * @param threads
     * @throws FileNotFoundException
     */
    // 基于椭圆曲线实现（多线程）从from_path中读取密文，拆分成多份子文件，多线程分别对子文件用key加密，将加密后的密文并发写入to_path
    // isEnc用来决定是执行加密还是解密
    public static void ECC_enc_dec_and_Write_mThreads(EccEnc ecc, Integer role, Boolean isEnc, String from_path, String to_path, int threads) throws FileNotFoundException {
    	File file = new File(from_path);
		String pathString; //保存服务端or客户端的根目录
		if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂
			System.out.println("线程数必须为2的幂次！");
			return;
		}
		//提取出保存文件的根目录
		if(role == 0) {//0代表server
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		
		//1.先读取文件长度
		long startTime = System.currentTimeMillis();
		try {
			int len = getLineNumber(file);
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
			split_File(pathString, Params.StageEnum.Encrypt.toString(), file, len, threads);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
		final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
		for(int j = 1; j <= threads; ++j) {
			int new_j = j;
			new Thread(() -> {
				try {
					FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Encrypt.toString() + new_j);
					BufferedReader temp_bReader = new BufferedReader(temp_reader);
					FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Encrypt.toString() + new_j + "out");
					BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
					String temp_line = null;
					String hash_String = null;
					String s = Params.pub_random.toString();
					org.bouncycastle.math.ec.ECPoint element;
					if(isEnc) {//isEnc为true，则逐行读取并加密
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							//加密后的内容写入outj文件中
							hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							element = ecc.BigIntegerToPoint(new BigInteger(hash_String, 16));
							temp_bWriter.write(Utils.bytesToHexString(ecc.encryptPoint(element).getEncoded(true)) + "\r\n");
						}
					} else {//isEnc为false，则逐行读取并解密
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并解密
							//解密后的内容写入outj文件中
							element = ecc.BigIntegerToPoint(new BigInteger(temp_line));
							temp_bWriter.write(Utils.bytesToHexString(ecc.decryptPoint(element).getEncoded(true)) + "\r\n");
						}
					}
					temp_bWriter.close();
					temp_bReader.close();
					cdl.countDown();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "t" + j).start();
		}
		//线程启动后调用countDownLatch方法
		try {
			cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
		//3.密文加密结束后，将所有产生的密文文件合成一份
		join_Files(pathString + Params.StageEnum.Encrypt.toString(), to_path, threads);
    }
    
    /**
     * 基于ECC的二次加密
     * @param ecc
     * @param role
     * @param isEnc
     * @param from_path
     * @param to_path
     * @param threads
     * @throws FileNotFoundException
     */
    public static void ECC_sec_enc_and_Write_mThreads(EccEnc ecc, Integer role, Boolean isEnc, String from_path, String to_path, int threads) throws FileNotFoundException {
		if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂
			System.out.println("线程数必须为2的幂次！");
			return;
		}
		//提取出保存文件的根目录
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {//0代表server
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		
		//1.先读取文件长度
		long startTime = System.currentTimeMillis();
		int len;
		try {
			File file = new File(from_path);
			len = getLineNumber(file);
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
			split_File(pathString, Params.StageEnum.Encrypt.toString(), file, len, threads);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
		final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
		for(int j = 1; j <= threads; ++j) {
			int new_j = j;
			new Thread(() -> {
				try {
					FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Encrypt.toString() + new_j);
					BufferedReader temp_bReader = new BufferedReader(temp_reader);
					FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Encrypt.toString() + new_j + "out");
					BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
					String temp_line = null;
					org.bouncycastle.math.ec.ECPoint element;
					if(isEnc) {//isEnc为true，则逐行读取并加密
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							//加密后的内容写入outj文件中
							//先把16进制字符串转成byte[]数组
							element = ecc.bytesToECPoint(Utils.hexStringToBytes(temp_line));
							element = ecc.encryptPoint(element);
							temp_bWriter.write(Utils.bytesToHexString(element.getEncoded(true)) + "\r\n");
						}
					} else {//isEnc为false，则逐行读取并解密
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并解密
							//解密后的内容写入outj文件中
							element = ecc.bytesToECPoint(Utils.hexStringToBytes(temp_line));
							element = ecc.decryptPoint(element);
							temp_bWriter.write(Utils.bytesToHexString(element.getEncoded(true)) + "\r\n");
						}
					}
					temp_bWriter.close();
					temp_bReader.close();
					cdl.countDown();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "t" + j).start();
		}
		//线程启动后调用countDownLatch方法
		try {
			cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
		//3.密文加密结束后，将所有产生的密文文件合成一份
		join_Files(pathString + Params.StageEnum.Encrypt.toString(), to_path, threads);
    }
    
    public static BloomFilter<String> encrypt_and_BloomWriter(Integer role, String from_path, Keys key, int threads) {
    	File file = new File(from_path);//存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
		    int len = getLineNumber(file);//获取文件行数，以得知元素个数
		    if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布隆过滤器
			BloomFilter<String> filter = new FilterBuilder()
					.expectedElements(len)
					.falsePositiveProbability(0.000000001)
					.hashFunction(HashMethod.Murmur3)
					.buildBloomFilter();
		    
			long startTime = System.currentTimeMillis(); //获取开始时间
			long endTime;
			int i;
			//多线程并发插入BF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_bloom.toString(), file, len, threads);
			//2. 对拆分后产生的文件进行并发访问加密,同时存入BF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() ->  {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_bloom.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						//String hash_String = null;
						//String s = Params.pub_random.toString();
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							// 得到哈希值h(s,x)再加密, 写入BF
							//hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							//filter.add(CommEnc.encrypt_BigInteger(new BigInteger(hash_String, 16), key).toString());
							filter.add(CommEnc.encrypt_BigInteger(new BigInteger(temp_line), key).toString());
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入BF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 基于ECC, 单次加密并存入BF
     * @param role
     * @param from_path
     * @param enc
     * @param threads
     * @return
     */
    public static BloomFilter<String> encrypt_and_BloomWriter(Integer role, String from_path, EccEnc enc, int threads) {
    	File file = new File(from_path);//存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
			int len = getLineNumber(file);//获取文件行数，以得知元素个数
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布隆过滤器
			BloomFilter<String> filter = new FilterBuilder()
					.expectedElements(len)
					.falsePositiveProbability(0.000000001)
					.hashFunction(HashMethod.Murmur3)
					.buildBloomFilter();
			
			long startTime = System.currentTimeMillis(); //获取开始时间
			//多线程并发插入BF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_bloom.toString(), file, len, threads);
			
			//2. 对拆分后产生的文件进行并发访问加密,同时存入BF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(int i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() ->  {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_bloom.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						org.bouncycastle.math.ec.ECPoint element;
						String hash_String = null;
						String s = Params.pub_random.toString();
						byte[] result;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							element = enc.BigIntegerToPoint(new BigInteger(hash_String, 16));
							result = enc.encryptPoint(element).getEncoded(true);
							filter.add(Utils.bytesToHexString(result));
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入BF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 基于ECC, 二次加密并存入BF
     * @param role
     * @param from_path
     * @param enc
     * @param threads
     * @return
     */
    public static BloomFilter<String> sec_encrypt_and_BloomWriter(Integer role, String from_path, EccEnc enc, int threads) {
    	File file = new File(from_path);//存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
			int len = getLineNumber(file);//获取文件行数，以得知元素个数
			if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布隆过滤器
			BloomFilter<String> filter = new FilterBuilder()
					.expectedElements(len)
					.falsePositiveProbability(0.000000001)
					.hashFunction(HashMethod.Murmur3)
					.buildBloomFilter();
			
			long startTime = System.currentTimeMillis(); //获取开始时间
			//多线程并发插入BF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_bloom.toString(), file, len, threads);
			
			//2. 对拆分后产生的文件进行并发访问加密,同时存入BF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(int i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() ->  {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_bloom.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						org.bouncycastle.math.ec.ECPoint element;
						byte[] result;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							result = Utils.hexStringToBytes(temp_line);
							element = enc.encryptPoint(enc.bytesToECPoint(result));
							filter.add(Utils.bytesToHexString(element.getEncoded(true)));
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入BF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 基于Pohlig-Hellman, 单次加密存入CF
     * @param role
     * @param from_path
     * @param key
     * @param threads
     * @return
     */
    public static CuckooFilter<byte[]> encrypt_and_CuckooWriter(Integer role, String from_path, Keys key, int threads) {
    	File file = new File(from_path);//存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
		    int len = getLineNumber(file);//获取文件行数，以得知元素个数
		    if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布谷鸟过滤器
			CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), len).
					withFalsePositiveRate(0.000000001).withHashAlgorithm(Algorithm.sha256).withExpectedConcurrency(threads).build();
		    
			long startTime = System.currentTimeMillis(); //获取开始时间
			//多线程并发插入CF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_cuckoo.toString(), file, len, threads);
			//2. 对拆分后产生的文件进行并发访问加密,同时存入CF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(int i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() -> {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_cuckoo.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						//String hash_String = null;
						//String s = Params.pub_random.toString();
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							//加密后的内容写入outj文件中
							//hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							// System.out.println("hash_string:" + hash_String);
							//filter.put(CommEnc.encrypt_BigInteger(new BigInteger(hash_String,16), key).toString().getBytes("utf-8"));
							filter.put(CommEnc.encrypt_BigInteger(new BigInteger(temp_line), key).toString().getBytes("utf-8"));
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入CF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 基于Pohlig-Hellman二次加密存入CF
     * @param role
     * @param from_path
     * @param key
     * @param threads
     * @return
     */
    public static CuckooFilter<byte[]> sec_encrypt_and_CuckooWriter(Integer role, String from_path, Keys key, int threads) {
    	File file = new File(from_path);//存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
		    int len = getLineNumber(file);//获取文件行数，以得知元素个数
		    if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布谷鸟过滤器
			CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), len).
					withFalsePositiveRate(0.000000001).withHashAlgorithm(Algorithm.sha256).withExpectedConcurrency(threads).build();
		    
			long startTime = System.currentTimeMillis(); //获取开始时间
			//多线程并发插入CF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_cuckoo.toString(), file, len, threads);
			//2. 对拆分后产生的文件进行并发访问加密,同时存入CF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(int i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() -> {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_cuckoo.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							filter.put(CommEnc.encrypt_BigInteger(new BigInteger(temp_line), key).toString().getBytes("utf-8"));
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入CF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 基于ECC, 对密文二次加密存入CF
     * @param role
     * @param from_path
     * @param enc
     * @param threads
     * @return
     */
    public static CuckooFilter<byte[]> sec_encrypt_and_CuckooWriter(Integer role, String from_path, EccEnc enc, int threads) {
    	File file = new File(from_path); //存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
		    int len = getLineNumber(file);//获取文件行数，以得知元素个数
		    if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    // 防止元素数量过少
		    int cuckoo_size = len < 64 ? 64 : len;
		    //初始化布谷鸟过滤器
			CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), cuckoo_size).
					withFalsePositiveRate(0.000000001).withHashAlgorithm(Algorithm.sha256).withExpectedConcurrency(threads).build();
			long startTime = System.currentTimeMillis(); //获取开始时间
			//多线程并发插入CF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_cuckoo.toString(), file, len, threads);
		    
			//2. 对拆分后产生的文件进行并发访问加密,同时存入CF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(int i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() ->  {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_cuckoo.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						byte[] temp;
						org.bouncycastle.math.ec.ECPoint element;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							//加密后的内容写入outj文件中
							temp = Utils.hexStringToBytes(temp_line);//十六进制密文转为byte[]
							element = enc.encryptPoint(enc.bytesToECPoint(temp));//byte[]转为椭圆曲线上的点并加密
							filter.put(element.getEncoded(true));//对点进行点压缩编码为byte[]
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入CF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    /**
     * 对明文哈希后单次加密并存入CF
     * @param role
     * @param from_path
     * @param enc
     * @param threads
     * @return
     */
    public static CuckooFilter<byte[]> encrypt_and_CuckooWriter(Integer role, String from_path, EccEnc enc, int threads) {
    	File file = new File(from_path); //存储客户端密文的文件
		String pathString; //保存服务端or客户端的根目录
		if(role == 0) {
			pathString = "server/";
		} else {
			pathString = "client/";
		}
		try {
		    int len = getLineNumber(file);//获取文件行数，以得知元素个数
		    if(len < threads) {// 避免极端情况
				threads = 1;
			}
		    //初始化布谷鸟过滤器
			CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), len).
					withFalsePositiveRate(0.000000001).withHashAlgorithm(Algorithm.sha256).withExpectedConcurrency(threads).build();
			long startTime = System.currentTimeMillis(); //获取开始时间
			//多线程并发插入CF
			//1. 密文拆分成线程数量个文件
			split_File(pathString, Params.StageEnum.Enc_cuckoo.toString(), file, len, threads);
		    
			//2. 对拆分后产生的文件进行并发访问加密,同时存入CF中
			final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
			for(int i = 1; i <= threads; ++i) {
				int new_j = i;
				new Thread(() ->  {
					try {
						FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Enc_cuckoo.toString() + new_j);
						BufferedReader temp_bReader = new BufferedReader(temp_reader);
						String temp_line = null;
						String hash_String = null;
						String s = Params.pub_random.toString();
						byte[] temp;
						org.bouncycastle.math.ec.ECPoint element;
						while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
							//加密后的内容写入outj文件中
							hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
							element = enc.BigIntegerToPoint(new BigInteger(hash_String, 16));
							filter.put(enc.encryptPoint(element).getEncoded(true));
						}
						temp_bReader.close();
						cdl.countDown();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}, "t" + i).start();
			}
			//线程启动后调用countDownLatch方法
			try {
				cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("加密元素并存入CF耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
			
			return filter;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    //对布谷鸟过滤器做交集大小查询
    public static int cuckoo_query_cardinality(Boolean isECC, String from, CuckooFilter<byte[]> filter) {
    	File file = new File(from);
		FileReader fileReader;
		int intersect_cardinality = 0;
		
		try {
			fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
		    String line = "";
		    long startTime = System.currentTimeMillis();
		    if(!isECC) {
		    	while ((line = bufferedReader.readLine()) != null) {
		        	if(filter.mightContain(line.getBytes("utf-8"))) {
						intersect_cardinality++;
					}
			    }
		    } else {
		        while ((line = bufferedReader.readLine()) != null) {
		        	if(filter.mightContain(Utils.hexStringToBytes(line))) {
						intersect_cardinality++;
					}
			    }
		    }
	        long endTime = System.currentTimeMillis(); //获取结束时间
			System.out.println("程序运行时间： "+(endTime-startTime)/1000 + "s" +
					 (endTime-startTime) % 1000 +"ms");
	        bufferedReader.close();
	    	return intersect_cardinality;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
    }
    
    //对布隆过滤器做交集大小查询
    public static int bloom_query_cardinality(String from, BloomFilter<String> filter) {
    	File file = new File(from);
		FileReader fileReader;
		int intersect_cardinality = 0;
		try {
			fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
		    String line = "";
		    long startTime = System.currentTimeMillis();
	    	while ((line = bufferedReader.readLine()) != null) {
	        	if(filter.contains(line)) {
					intersect_cardinality++;
				}
	    	}
		    long endTime = System.currentTimeMillis(); //获取结束时间
			System.out.println("程序运行时间： "+(endTime-startTime)/1000 + "s" +
					 (endTime-startTime) % 1000 +"ms");
	        bufferedReader.close();
	    	return intersect_cardinality;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
    }
}
/**
//* 多线程哈希再加密元素的方法, 其中加密是椭圆曲线加密
//* @param role
//* @param isBigSet true大集合一方专用: 二元组集合生成函数; false小集合一方专用: 只生成密文
//* @param from_path
//* @param to_path
//* @param ecc
//* @param threads
//* @throws FileNotFoundException
//*/
//public static void hash_prefix_enc_mThreads(Integer role, Boolean isBigSet, String from_path, String to_path, EccEnc ecc, int threads) throws FileNotFoundException {
//	if(threads > 0 && (threads & (threads - 1)) != 0) {//判断threads是否为2的幂
//		System.out.println("线程数必须为2的幂次！");
//		return;
//	}
//	//提取出保存文件的根目录
//	String pathString; //保存服务端or客户端的根目录
//	if(role == 0) {//0代表server
//		pathString = "server/";
//	} else {
//		pathString = "client/";
//	}
//	//1.先读取文件长度
//	long startTime = System.currentTimeMillis();
//	int len;
//	File file = new File(from_path);
//	try {
//		len = getLineNumber(file);
//		split_File(pathString, Params.StageEnum.Prefix_filter.toString(), file, len, threads);//将目标文件拆分成threads份
//	} catch (IOException e1) {
//		e1.printStackTrace();
//	}
//	//2.开始多线程访问不同文件进行加密,并将加密结果写入同一个toPath文件中
//	final CountDownLatch cdl = new CountDownLatch(threads);//参数为线程个数
//	for(int j = 1; j <= threads; ++j) {
//		int new_j = j;
//		new Thread(() ->  {
//			try {
//				FileReader temp_reader = new FileReader(pathString + Params.StageEnum.Prefix_filter.toString() + new_j);
//				BufferedReader temp_bReader = new BufferedReader(temp_reader);
//				FileWriter temp_writer = new FileWriter(pathString + Params.StageEnum.Prefix_filter.toString() + new_j + "out");
//				BufferedWriter temp_bWriter = new BufferedWriter(temp_writer);
//				String temp_line = null;
//				String hash_prefix = null;
//				String hash_String = null;
//				String s = Params.pub_random.toString();
//				org.bouncycastle.math.ec.ECPoint element;
//				if (isBigSet) {// 如果是大集合
//					while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
//						// 计算哈希前缀h(x)[0:w]
//						hash_prefix = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(temp_line, Params.prefix_len));
//						System.out.println("hash_prefix:" + hash_prefix);
//						// 得到哈希值h(s,x)再加密, 并将哈希前缀与密文组成二元组(h(x)[0:w],h(s,x))写入文件
//						hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
//						element = ecc.BigIntegerToPoint(new BigInteger(hash_String, 16));
//						temp_bWriter.write(hash_prefix + "," + Utils.bytesToHexString(ecc.encryptPoint(element).getEncoded(true)) + "\r\n");
//					}
//				} else {
//					while((temp_line = temp_bReader.readLine()) != null) {//逐行读取元素并加密
//						// 得到哈希值h(s,x)再加密,写入文件
//						hash_String = Utils.bytesToHexString(PreReduce.Hash_and_Get_Bits(s + temp_line, 256));
//						element = ecc.BigIntegerToPoint(new BigInteger(hash_String, 16));
//						temp_bWriter.write(Utils.bytesToHexString(ecc.encryptPoint(element).getEncoded(true)) + "\r\n");
//					}
//				}
//				temp_bWriter.close();
//				temp_bReader.close();
//				cdl.countDown();
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}, "t" + j).start();
//	}
//	//线程启动后调用countDownLatch方法
//	try {
//		cdl.await();//需要捕获异常，当其中线程数为0时这里才会继续运行
//	} catch (InterruptedException e) {
//		e.printStackTrace();
//	}
//	long endTime = System.currentTimeMillis();
//	System.out.println("加密元素耗时:" + (endTime - startTime) / 1000 + "s" + (endTime - startTime) % 1000 + "ms");	
//	//3.密文加密结束后，将所有产生的密文文件合成一份
//	join_Files(pathString + Params.StageEnum.Prefix_filter.toString(), to_path, threads);
//}
