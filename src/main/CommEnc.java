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
	// ���õ�BigInteger������Ϊ����
	private static final BigInteger ZERO = BigInteger.valueOf(0);
	private static final BigInteger ONE = BigInteger.valueOf(1);
	private static final BigInteger TWO = BigInteger.valueOf(2);

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidCipherTextException, ClassNotFoundException {
		// �������ɿ���Ϊ�������p
//		  for(int i = 0; i < 25; ++i) { 
//			  BigInteger p = generate_P(1024);
//			  System.out.println("����Ϊ���С������p��" + p);
//		      writeTxt("result2.txt", p.toString() + "\n"); 
//		  }
		
		//Keys key = new Keys(new BigInteger("109389738527156163781666016043624954506167396213790471900473615303254345069789140271678274396471506523534428108841632308527132993195355811447184287222295642859559897848481294362354585835678404095867204022374212541095338248886894966219260223907148852888494777888230291071238617176103936230311318047508645983823"));
		Keys key = generate_Key();//��Կ�������
//		if(key == null) {//��Կ����ʧ��
//			System.out.println("��Կ����ʧ��");
//			return ;
//		}
		//key.setA(new BigInteger("60281370649714090072935323458596731864745657319398230065227508480973639984235755918746246970752871868527298867511444853197642292328868821321780970753370022137111168635269849468311546095081822840104269799138951750928068762410152437926387447560872860521162522264613930876838261923614042078345715432899078875939"));
		//key.setA_Inv(new BigInteger("58234400304257107556386981089927077869519522480794493770356603287763675156684620030433420730466255837958222412977293777756165143210437970032478071041448242846233113793472913588823241699698565862016884733912534984621160957297669492809320625872735772150197928815760085889904582187530975332256507215858351988755"));
		
		Keys key2 = new Keys(key.getP());//��һ�����뷽����Կ�������p��Ҫ�����ģ���Կ�������ɼ���
		generate_Keys(key2);
		System.out.println("key2������Ⱥ:" + key2.getP().toString());
		System.out.println("key2������Կ:" + key2.getA().toString());
		System.out.println("key2������Կ:" + key2.getAInv().toString());
		
		//������ת���ɿɼ��ܵ�utf-8��ʽ������utf-8תΪ����
		String data = "�����к�����ѧԺ·37��";
		BigInteger cipher = encrypt_Data(data, key);//����data
		
		BigInteger message = decrypt_Data(cipher, key);
		byte[] block = message.toByteArray();
		String final_decryption = new String(block,"utf-8");
		System.out.println("���ν��ܺ�utf-8����������:" + final_decryption);
	
		//�����������
		//ʹ��ʱ�Ƚ�����ת��Ϊbyte[]���飬�ٴ��벼���������
		//CuckooFilter filter = null;
		// create
		CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), 2000000).
				withFalsePositiveRate(0.000000000001).withHashAlgorithm(Algorithm.sha256).build();
		filter.put(block);
		//���������л���filter���棬Ȼ����
		//ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		//byteArrayOutputStream.write();
		
		ObjectOutputStream objectOutputStream = 
		        new ObjectOutputStream( new FileOutputStream( new File("filter.txt") ) );
		    objectOutputStream.writeObject(filter);
		    
	    System.out.println("���л��ɹ����Ѿ�����filter.txt�ļ�");
		 
		ObjectInputStream objectInputStream = 
		        new ObjectInputStream( new FileInputStream( new File("filter.txt") ) );
		CuckooFilter<byte[]> filter2 = (CuckooFilter<byte[]>) objectInputStream.readObject();
		        objectInputStream.close();
		if(filter2.equals(filter)) { 
			System.out.println("�����л��ɹ�");
		}
	}
	
	public static BigInteger encrypt_BigInteger(BigInteger num, Keys key) {//�����ݽ��м���
		return num.modPow(key.getA(), key.getP());
	}
	
	public static BigInteger decrypt_BigInteger(BigInteger num, Keys key) {//�����ݽ��н���
		return num.modPow(key.getAInv(), key.getP());
	}
	
	
	public static BigInteger encrypt_Data(String data, Keys key) {//�����ݽ��м���
		byte[] block;
		BigInteger cipher = null;
		try {
			block = data.getBytes("utf-8");
			//System.out.println("����ת��utf-8:" + Arrays.toString(block));
			BigInteger res = new BigInteger(1, block);//�����ı��utf-8���룬�ٱ��BigInteger
	        if (res.compareTo(key.getP()) >= 0) {
	            throw new DataLengthException("input too large for Pohlig-Hellman cipher.");
	        }
			cipher = res.modPow(key.getA(), key.getP());//��ɼ���
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return cipher;
	}
	
	public static BigInteger decrypt_Data(BigInteger cipher, Keys key) {//�����ݽ��м���
		return cipher.modPow(key.getAInv(), key.getP());
	}
	
	public static Keys generate_Key() {//������Կ��p,a,a_inv
		Keys key = null;
		try {
			// �ȴ��ļ��д洢�õ����������ѡȡ1��
			BigInteger[] primes = toArrayByFileReader("result.txt");//��ȫ��ȡ����
			SecureRandom ran = SecureRandom.getInstance("SHA1PRNG");//�������������������һ������p
			int index = ran.nextInt(50);
			BigInteger p = primes[index];//ѡ������p
			key = new Keys(p);//��ʼ��key.p
			generate_Keys(key);//���ɼ�����Կ�ͽ�����Կ
			System.out.println("key1������Ⱥ:" + key.getP().toString());
			System.out.println("key1������Կ:" + key.getA().toString());
			System.out.println("key1������Կ:" + key.getAInv().toString());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return key;
	}
	
	//��Ԫ�ؼ��϶��յ��Ĳ�������������м��������ؼ������Ľ�����С
	public static int check_Intersection_Cardinality(BigInteger[] elements, CuckooFilter<byte[]> cf) {
		int count = 0;
		long len = elements.length;//��ȡԪ�ؼ��ϴ�С
		for(int i = 0; i < len; ++i) {
			byte[] block = elements[i].toByteArray();
			if(cf.mightContain(block)) {
				count++;
			}
		}
		return count;
	}
	
	//���ڽ�һϵ��BigIntegerԪ�ط��벼�����������
	public static CuckooFilter<byte[]> put_in_CF(BigInteger[] elements, double fpp, int size) {//Ԫ�ؼ���,������,�����Ԫ�ظ���
		long len = elements.length;//��ȡԪ�ؼ��ϴ�С
		CuckooFilter<byte[]> filter = new CuckooFilter.Builder<>(Funnels.byteArrayFunnel(), size).
				withFalsePositiveRate(fpp).withHashAlgorithm(Algorithm.sha256).build();
		for(int i = 0; i < len; ++i) {
			byte[] block = elements[i].toByteArray();
			filter.put(block);
		}
		System.out.println("Filter has " + filter.getCount() + " items");
		//������
		System.out.println("Filter is " + String.format("%.0f%%", filter.getLoadFactor() * 100) + " loaded");
		return filter;
	}

	public static BigInteger quick_Pow(BigInteger m, BigInteger a, BigInteger p) {// ����ģ���㷨
		BigInteger result = ONE;
		BigInteger k = a;
		while (!k.equals(ZERO)) {
			if (k.testBit(0)) {// ���kĩλ��1
				result = result.multiply(m).remainder(p);
			}
			m = (m.multiply(m)).remainder(p);
			k = k.shiftRight(1);
		}
		return result;
	}

	// BigInteger��һ�ֶ������ͣ����Կ�ֱ����Ϊ���ô���
	
	public static void generate_Keys(Keys key) throws NoSuchAlgorithmException {
		// ������Կ��a,a^(-1)�ĺ���,��Կa,a^(-1)��G_(p-1),
		// ���Ȼ�ȡp�ı���λ����Ȼ��������һλ����һ��Χ�ڵ��������������p-1��Χ��
		int len = key.getP().bitLength();
		System.out.println("p�Ķ����Ƴ���:" + len);
		SecureRandom ran = SecureRandom.getInstance("SHA1PRNG");
		key.setA(new BigInteger(len - 1, ran));// a
		if(!key.getA().testBit(0)) {//���������ż�����ͼ�1������
			key.setA(key.getA().add(BigInteger.ONE));
		}
		BigInteger p_minus = key.getP().subtract(ONE);
		key.setA_Inv(key.getA().modInverse(p_minus));// a^(-1) mod (p-1)
	}

	public static BigInteger generate_P(int length) throws NoSuchAlgorithmException {
		// ���ɳ���Ϊlength������p��������q=(p-1)/2Ҳ������
		SecureRandom ran = SecureRandom.getInstance("SHA1PRNG");
		// BigInteger p = new BigInteger(length, ran); //���������0~2^(length)-1�������
		BigInteger p = new BigInteger(length, 1, ran);// �Խϴ����������������p
		//BigInteger pBigInteger = new BigInteger(length, ran);
		BigInteger q;
		for (;;) {// ��һ��������p
			if (p.isProbablePrime(40) == true) {// �ҵ�����q
				System.out.println("��������p0��" + p + "����Ϊ:" + p.bitLength());
				q = p.subtract(ONE);
				q = q.divide(TWO);
				System.out.println("q=(p-1)/2��" + q);
				if (q.isProbablePrime(40) == true)// �ж�q=(p-1)/2�Ƿ�Ϊ�������ǵĻ������ҵ�������p
					break;
			}
			p = new BigInteger(1024, 1, ran);
		}
		return p;
	}

	public static void writeTxt(String txtPath, String content) {// д�ļ���׷��ģʽ
		try {
			FileWriter writer;
			writer = new FileWriter(txtPath, true);
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public static BigInteger[] toArrayByFileReader(String name) {// ���ļ��ж�ȡ����
		ArrayList<String> arrayList = new ArrayList<>();
		try {
			FileReader fr;
			fr = new FileReader(name);
			BufferedReader bf = new BufferedReader(fr);
			String str;
			// ���ж�ȡ�ַ���
			while ((str = bf.readLine()) != null) {
				arrayList.add(str);
			}
			bf.close();
			fr.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		// ��ArrayList�д洢���ַ������д���
		int length = arrayList.size();
		BigInteger[] array = new BigInteger[length];
		for (int i = 0; i < length; i++) {
			String s = arrayList.get(i);
			array[i] = new BigInteger(s);// ʹ�ù��캯�����Խ��ַ�����ʼ��ΪBigInteger
		}
		return array;
	}
	
	public static ArrayList<String> toStrArrayByFileReader(String name) {// ���ļ��ж�ȡԪ��
		ArrayList<String> arrayList = new ArrayList<>();
		try {
			FileReader fr;
			fr = new FileReader(name);
			BufferedReader bf = new BufferedReader(fr);
			String str;
			// ���ж�ȡ�ַ���
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
