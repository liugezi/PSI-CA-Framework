import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;
import com.github.mgunlogson.cuckoofilter4j.Utils.Algorithm;
import com.google.common.hash.Funnels;

public class CommEnc {
	// 常用的BigInteger，声明为常量
	private static final BigInteger ZERO = BigInteger.valueOf(0);
	private static final BigInteger ONE = BigInteger.valueOf(1);
	private static final BigInteger TWO = BigInteger.valueOf(2);

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidCipherTextException, ClassNotFoundException {
		// 用于生成可作为域的素数p
//		  for(int i = 0; i < 25; ++i) { 
//			  BigInteger p = generate_P(1024);
//			  System.out.println("可作为域大小的素数p：" + p);
//		      writeTxt("result2.txt", p.toString() + "\n"); 
//		  }
		
		//Keys key = new Keys(new BigInteger("109389738527156163781666016043624954506167396213790471900473615303254345069789140271678274396471506523534428108841632308527132993195355811447184287222295642859559897848481294362354585835678404095867204022374212541095338248886894966219260223907148852888494777888230291071238617176103936230311318047508645983823"));
		Keys key = generate_Key();//密钥管理对象
//		if(key == null) {//密钥生成失败
//			System.out.println("密钥生成失败");
//			return ;
//		}
		//key.setA(new BigInteger("60281370649714090072935323458596731864745657319398230065227508480973639984235755918746246970752871868527298867511444853197642292328868821321780970753370022137111168635269849468311546095081822840104269799138951750928068762410152437926387447560872860521162522264613930876838261923614042078345715432899078875939"));
		//key.setA_Inv(new BigInteger("58234400304257107556386981089927077869519522480794493770356603287763675156684620030433420730466255837958222412977293777756165143210437970032478071041448242846233113793472913588823241699698565862016884733912534984621160957297669492809320625872735772150197928815760085889904582187530975332256507215858351988755"));
		
		Keys key2 = new Keys(key.getP());//另一个参与方的密钥对象，因此p是要公开的，密钥各自生成即可
		generate_Keys(key2);
		System.out.println("key2素数阶群:" + key2.getP().toString());
		System.out.println("key2加密密钥:" + key2.getA().toString());
		System.out.println("key2解密密钥:" + key2.getAInv().toString());
		
		//将明文转换成可加密的utf-8形式，再由utf-8转为数字
		String data = "北京市海淀区学院路37号";
		BigInteger cipher = encrypt_Data(data, key);//加密data
		
		BigInteger message = decrypt_Data(cipher, key);
		byte[] block = message.toByteArray();
		String final_decryption = new String(block,"utf-8");
		System.out.println("两次解密后，utf-8解码后的明文:" + final_decryption);
	
		//布谷鸟过滤器
		//使用时先将密文转换为byte[]数组，再存入布谷鸟过滤器
		//CuckooFilter filter = null;
		// create
		CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), 2000000).
				withFalsePositiveRate(0.000000000001).withHashAlgorithm(Algorithm.sha256).build();
		filter.put(block);
		//考虑用序列化将filter保存，然后发送
		//ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		//byteArrayOutputStream.write();
		
		ObjectOutputStream objectOutputStream = 
		        new ObjectOutputStream( new FileOutputStream( new File("filter.txt") ) );
		    objectOutputStream.writeObject(filter);
		    
	    System.out.println("序列化成功！已经生成filter.txt文件");
		 
		ObjectInputStream objectInputStream = 
		        new ObjectInputStream( new FileInputStream( new File("filter.txt") ) );
		CuckooFilter<byte[]> filter2 = (CuckooFilter<byte[]>) objectInputStream.readObject();
		        objectInputStream.close();
		if(filter2.equals(filter)) { 
			System.out.println("反序列化成功");
		}
	}
	
	public static BigInteger encrypt_BigInteger(BigInteger num, Keys key) {//对数据进行加密
		return num.modPow(key.getA(), key.getP());
	}
	
	public static BigInteger decrypt_BigInteger(BigInteger num, Keys key) {//对数据进行解密
		return num.modPow(key.getAInv(), key.getP());
	}
	
	
	public static BigInteger encrypt_Data(String data, Keys key) {//对数据进行加密
		byte[] block;
		BigInteger cipher = null;
		try {
			block = data.getBytes("utf-8");
			//System.out.println("明文转成utf-8:" + Arrays.toString(block));
			BigInteger res = new BigInteger(1, block);//把明文变成utf-8编码，再变成BigInteger
	        if (res.compareTo(key.getP()) >= 0) {
	            throw new DataLengthException("input too large for Pohlig-Hellman cipher.");
	        }
			cipher = res.modPow(key.getA(), key.getP());//完成加密
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return cipher;
	}
	
	public static BigInteger decrypt_Data(BigInteger cipher, Keys key) {//对数据进行加密
		return cipher.modPow(key.getAInv(), key.getP());
	}
	
	public static Keys generate_Key() {//生成密钥的p,a,a_inv
		Keys key = null;
		try {
			// 先从文件中存储好的素数中随机选取1个
			BigInteger[] primes = toArrayByFileReader("result.txt");//先全部取出来
			SecureRandom ran = SecureRandom.getInstance("SHA1PRNG");//生成随机数决定采用哪一个素数p
			int index = ran.nextInt(50);
			BigInteger p = primes[index];//选定素数p
			key = new Keys(p);//初始化key.p
			generate_Keys(key);//生成加密密钥和解密密钥
			System.out.println("key1素数阶群:" + key.getP().toString());
			System.out.println("key1加密密钥:" + key.getA().toString());
			System.out.println("key1解密密钥:" + key.getAInv().toString());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return key;
	}
	
	//将元素集合对收到的布谷鸟过滤器进行检索，返回检索到的交集大小
	public static int check_Intersection_Cardinality(BigInteger[] elements, CuckooFilter<byte[]> cf) {
		int count = 0;
		long len = elements.length;//获取元素集合大小
		for(int i = 0; i < len; ++i) {
			byte[] block = elements[i].toByteArray();
			if(cf.mightContain(block)) {
				count++;
			}
		}
		return count;
	}
	
	//用于将一系列BigInteger元素放入布谷鸟过滤器中
	public static CuckooFilter<byte[]> put_in_CF(BigInteger[] elements, double fpp, int size) {//元素集合,假阳率,拟插入元素个数
		long len = elements.length;//获取元素集合大小
		CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), size).
				withFalsePositiveRate(fpp).withHashAlgorithm(Algorithm.sha256).build();
		for(int i = 0; i < len; ++i) {
			byte[] block = elements[i].toByteArray();
			filter.put(block);
		}
		System.out.println("Filter has " + filter.getCount() + " items");
		//负载率
		System.out.println("Filter is " + String.format("%.0f%%", filter.getLoadFactor() * 100) + " loaded");
		return filter;
	}

	public static BigInteger quick_Pow(BigInteger m, BigInteger a, BigInteger p) {// 快速模幂算法
		BigInteger result = ONE;
		BigInteger k = a;
		while (!k.equals(ZERO)) {
			if (k.testBit(0)) {// 如果k末位是1
				result = result.multiply(m).remainder(p);
			}
			m = (m.multiply(m)).remainder(p);
			k = k.shiftRight(1);
		}
		return result;
	}

	// BigInteger是一种对象类型，所以可直接作为引用传递
	
	public static void generate_Keys(Keys key) throws NoSuchAlgorithmException {
		// 生成密钥：a,a^(-1)的函数,密钥a,a^(-1)∈G_(p-1),
		// 首先获取p的比特位数，然后生成这一位数减一范围内的随机数即可满足p-1范围内
		int len = key.getP().bitLength();
		System.out.println("p的二进制长度:" + len);
		SecureRandom ran = SecureRandom.getInstance("SHA1PRNG");
		key.setA(new BigInteger(len - 1, ran));// a
		if(!key.getA().testBit(0)) {//如果发现是偶数，就加1变奇数
			key.setA(key.getA().add(BigInteger.ONE));
		}
		BigInteger p_minus = key.getP().subtract(ONE);
		key.setA_Inv(key.getA().modInverse(p_minus));// a^(-1) mod (p-1)
	}

	public static BigInteger generate_P(int length) throws NoSuchAlgorithmException {
		// 生成长度为length的素数p，且满足q=(p-1)/2也是素数
		SecureRandom ran = SecureRandom.getInstance("SHA1PRNG");
		// BigInteger p = new BigInteger(length, ran); //这个是生成0~2^(length)-1的随机数
		BigInteger p = new BigInteger(length, 1, ran);// 以较大可能性来生成素数p
		//BigInteger pBigInteger = new BigInteger(length, ran);
		BigInteger q;
		for (;;) {// 找一个大素数p
			if (p.isProbablePrime(40) == true) {// 找到素数q
				System.out.println("发现素数p0：" + p + "长度为:" + p.bitLength());
				q = p.subtract(ONE);
				q = q.divide(TWO);
				System.out.println("q=(p-1)/2：" + q);
				if (q.isProbablePrime(40) == true)// 判断q=(p-1)/2是否为素数，是的话，就找到了素数p
					break;
			}
			p = new BigInteger(1024, 1, ran);
		}
		return p;
	}

	public static void writeTxt(String txtPath, String content) {// 写文件，追加模式
		try {
			FileWriter writer;
			writer = new FileWriter(txtPath, true);
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public static BigInteger[] toArrayByFileReader(String name) {// 从文件中读取素数
		ArrayList<String> arrayList = new ArrayList<>();
		try {
			FileReader fr;
			fr = new FileReader(name);
			BufferedReader bf = new BufferedReader(fr);
			String str;
			// 按行读取字符串
			while ((str = bf.readLine()) != null) {
				arrayList.add(str);
			}
			bf.close();
			fr.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		// 对ArrayList中存储的字符串进行处理
		int length = arrayList.size();
		BigInteger[] array = new BigInteger[length];
		for (int i = 0; i < length; i++) {
			String s = arrayList.get(i);
			array[i] = new BigInteger(s);// 使用构造函数可以将字符串初始化为BigInteger
		}
		return array;
	}
	
	public static ArrayList<String> toStrArrayByFileReader(String name) {// 从文件中读取元素
		ArrayList<String> arrayList = new ArrayList<>();
		try {
			FileReader fr;
			fr = new FileReader(name);
			BufferedReader bf = new BufferedReader(fr);
			String str;
			// 按行读取字符串
			while ((str = bf.readLine()) != null) {
				arrayList.add(str);
			}
			bf.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return arrayList;
	}
}
