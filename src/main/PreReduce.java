import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.*;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider.HashMethod;

public class PreReduce {

	public static void main(String[] args) {
		//匹配筛选实验
		//先生成几个小集合 n1={2^10,2^11,2^12,2^13,2^14}
		SecureRandom ran;
		String nameString;
		long[] arr = {(long) Math.pow(2, 6), (long) Math.pow(2, 7), (long) Math.pow(2, 8), (long) Math.pow(2, 9), (long) Math.pow(2, 10)};
		long[] arr2 = {(long) Math.pow(2, 15), (long) Math.pow(2, 16), (long) Math.pow(2, 17), (long) Math.pow(2, 18), (long) Math.pow(2, 19), (long) Math.pow(2, 20)};
		//生成测试数据集
		try {
			for(int i = 0; i < arr.length; ++i) {//
				nameString =  arr[i] + ".txt";
				File file = new File(nameString);
				if(file.exists()) continue;
				for(long j = 0; j < arr[i]; ++j) {
					ran = SecureRandom.getInstance("SHA1PRNG");
					CommEnc.writeTxt(nameString, createMobile(ran.nextInt(3)) + "\n");
					if(j % 100 == 0) {
						System.out.println("第" + i + "个文件的第" + j + "个手机号已生成");
					}
				}
			}
			
			//再生成几个大集合 n2={2^16,2^18,2^20,2^22,2^24}
			for(int i = 0; i < arr2.length; ++i) {//
				nameString =  arr2[i] + ".txt";
				File file = new File(nameString);
				if(file.exists()) continue;
				for(long j = 0; j < arr2[i]; ++j) {
					ran = SecureRandom.getInstance("SHA1PRNG");
					CommEnc.writeTxt(nameString, createMobile(ran.nextInt(3)) + "\n");
					if(j % 10000 == 0) {
						System.out.println("第" + i + "个文件的第" + j + "个手机号已生成");
					}
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}		
		
		//对大集合也进行哈希, 也截取log(n1)+1, log(n1)+2, log(n1)+3, log(n1)+4 bits
		
		//分别求交集，观察交集大小，比对不同长度下的过滤效果
		//对小集合哈希, 然后分别截取log(n1)+1, log(n1)+2, log(n1)+3, log(n1)+4
				//先做个2^10的
		//String out_nameString;
		String lineString = "";
		byte[] result;
		try {
			File file;
			FileReader fileReader;
			BufferedReader bReader;
			for(int i = 0; i < arr.length; ++i) {//n1={2^6,2^7,2^8,2^9,2^10}
				file = new File(arr[i] + ".txt");
				
				for(int j = 1; j <= 8; ++j) {//尝试log(n1)+1, log(n1)+2, log(n1)+3, log(n1)+4, log(n1)+5这几种过滤
					fileReader = new FileReader(file);
					bReader = new BufferedReader(fileReader);
					int cut_len = (int)(log2(arr[i]) + j);
					//out_nameString = arr[i] + "_" + cut_len + ".txt";
					BloomFilter<String> filter = new FilterBuilder()
							.expectedElements((int) arr[i])
							.falsePositiveProbability(0.0000000001)
							.hashFunction(HashMethod.Murmur3)
							.buildBloomFilter();
					//开始逐行读取文件内容并哈希，截取其前log(n1)+j比特
					while ((lineString = bReader.readLine()) != null) {//读取文件
						result = Hash_and_Get_Bits(lineString, cut_len);
						filter.add(result);//存入BF
					}
					int cnt;//用于计数有多少个相同的元素
					//在大集合文件中做查询，输出匹配数量
					for(int i2 = 0; i2 < arr2.length; ++i2) {
						File file2 = new File(arr2[i2] + ".txt");
						FileReader fileReader2 = new FileReader(file2);
						BufferedReader bReader2 = new BufferedReader(fileReader2);
						cnt = 0;//每个文件都置0
						for(int j2 = 0; j2 < arr2[i2]; ++j2) {
							while((lineString = bReader2.readLine()) != null) {
								result = Hash_and_Get_Bits(lineString, cut_len);
								if(filter.contains(result)) {
									cnt++;
								}
							}
						}
						System.out.println("文件" + (int)arr2[i2] + "在中经过BF查询有" + cnt + "个与" + arr[i] + ".txt" + "前缀长度为" + cut_len + "时相同的元素");
						bReader2.close();
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void Pre_Filter() {//根据对方元素数量的数量级以及自己的元素数量的数量级差异，决定要生成的前缀长度，并产生相应的BF
		
	}
	
	public static void Rev_Pre_Filter() {//接收对方发来的BF, 并接收对方的元素前缀长度.
		
	}
	
	public static String byteToBit(byte b) {
		return ""
				+ (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
				+ (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
				+ (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
				+ (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
	}
	
	public static double log2(double N) {
		return Math.log(N) / Math.log(2);//
	}
	
	public static byte[] Hash_and_Get_Bits(String txt, int b) {//txt为要哈希的明文字符串, b为要获得的比特位数
		byte[] output = null;
		try {
			MessageDigest mDigest;
			mDigest = MessageDigest.getInstance("SHA-256");
			mDigest.update(txt.getBytes());
			byte[] digest = mDigest.digest();
			//System.out.println("输出sha256的16进制结果" + Utils.bytesToHexString(digest));
			if(b == 256) return digest;
			
			int group = b % 8 == 0 ? b / 8 : b / 8 + 1;
			output = new byte[group];
			for(int i = 0; i < group; ++i) {
				output[i] = digest[i];
			}
			
			if(b % 8 != 0) {
				int left = b % 8;
				int shift = 8 - left;
				for(int j = 0; j < shift; ++j) {
					output[group - 1] = (byte) (output[group - 1] >> 1);
				}
			}
//			result = new byte[8 * group];//长度为b的比特数组 
//			
//			for(int i = 0; i < group; ++i) {
//				for(int j = 7; j >= 0; j--) {
//					result[i * 8 + j] = (byte)(digest[i] & 1);
//					digest[i] = (byte) (digest[i] >> 1);
//				}
//			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	/**
     * 返回手机号码 
     */
	//中国移动
	public static final String[] CHINA_MOBILE = {
            "134", "135", "136", "137", "138", "139", "150", "151", "152", "157", "158", "159",
            "182", "183", "184", "187", "188", "178", "147", "172", "198" };
	//中国联通
    public static final String[] CHINA_UNICOM = {
            "130", "131", "132", "145", "155", "156", "166", "171", "175", "176", "185", "186", "166"
    };
    //中国电信
    public static final String[] CHINA_TELECOME = {
            "133", "149", "153", "173", "177", "180", "181", "189", "199"
    };
	
    /**
     * 生成手机号
     *
     * @param op 0 移动 1 联通 2 电信
     */
    public static String createMobile(int op) {
        StringBuilder sb = new StringBuilder();
        SecureRandom ran;
		try {
			ran = SecureRandom.getInstance("SHA1PRNG");
			String mobile01;//手机号前三位
	        int temp;
	        switch (op) {
	            case 0:
	                mobile01 = CHINA_MOBILE[ran.nextInt(CHINA_MOBILE.length)];
	                break;
	            case 1:
	                mobile01 = CHINA_UNICOM[ran.nextInt(CHINA_UNICOM.length)];
	                break;
	            case 2:
	                mobile01 = CHINA_TELECOME[ran.nextInt(CHINA_TELECOME.length)];
	                break;
	            default:
	                mobile01 = "op标志位有误！";
	                break;
	        }
	        if (mobile01.length() > 3) {
	            return mobile01;
	        }
	        sb.append(mobile01);
	        //生成手机号后8位
	        for (int i = 0; i < 8; i++) {
	            temp = ran.nextInt(10);
	            sb.append(temp);
	        }
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		return sb.toString();
    }
}
