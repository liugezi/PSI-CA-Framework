import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

public class EccEnc {
	//希望这个类能封装不同的椭圆曲线加解密算法, 初始化只需指定曲线名即可
	public static void main(String[] args) {
			EccEnc eccEnc = new EccEnc();
			eccEnc.generateKey();
			String telString = "13382958827";//待加密数据
			BigInteger I = new BigInteger(telString);
			ECPoint encPoint = eccEnc.G.multiply(I);//非压缩形式加密
			
			byte[] result = encPoint.getEncoded(false);
			
			System.out.println("加密前的点: " + new String(result));
			
			encPoint = encPoint.multiply(eccEnc.key);
			result = encPoint.getEncoded(false);
			
			System.out.println("一次加密后的点: " + new String(result));
			
			encPoint = eccEnc.bytesToECPoint(result);//先将byte[]形式密文转为椭圆曲线上的点
			
			//encPoint = encPoint.multiply(eccEnc.key2);//二次加密
			
			result = encPoint.getEncoded(true);
			
			System.out.println("二次加密后的点: " + new String(result));
			
			//ECPoint encPoint2 = eccEnc.bytesToECPoint(result);
			//encPoint = encPoint.multiply(eccEnc.key2_inv);
			
			result = encPoint.getEncoded(true);
			
			System.out.println("一次解密后的点: " + new String(result));
			
			String tempString = new String(result);
			
			byte[] result2 = tempString.getBytes(); 
			
			System.out.println("看看string恢复出来的byte数组对不对: " + new String(result2));
			
			encPoint = encPoint.multiply(eccEnc.key_inv);
			result = encPoint.getEncoded(true);
			
			System.out.println("2次解密后的点: " + new String(result));
			
			String hexOutputString = Utils.bytesToHexString(result);
			
			System.out.println("2次解密后的点(16进制): " + hexOutputString);
			
			result = Utils.hexStringToBytes(hexOutputString);
			
			System.out.println("2次解密后的点: " + new String(result));
			
			//Vector<String> vector = ECNamedCurveTable.getNames();
			
			Enumeration<String> enumeration = ECNamedCurveTable.getNames();
			System.out.println("查看可用的椭圆曲线列表:");
			int cnt = 1;
			while(enumeration.hasMoreElements()) {
				System.out.println("第" + cnt++ + "个:" + enumeration.nextElement());
			}
			
	}
	
	public EccEnc() {//构造函数
		generateKey();
	}
	
	//先基于BouncyCastle实现一个简单的ECC
	private BigInteger key;
	private BigInteger key_inv;
	private BigInteger key2;
	private BigInteger key2_inv;
	private ECPoint G;

	private void generateKey() {
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("prime256v1");	
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("sm2p256v1");
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("K-283");
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("K-233");
		//ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp224r1");
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(Params.eccEnum.getEccName());
		BigInteger n = ecSpec.getN();
		G = ecSpec.getG();
		key = org.bouncycastle.util.BigIntegers.createRandomInRange(BigInteger.ONE, 
    			n.subtract(BigInteger.ONE), new SecureRandom());
		key_inv = key.modInverse(n);
	}
	
	public ECPoint hashToPoint(byte[] hash) {//将元素的哈希值转为Point
		BigInteger element = new BigInteger(hash);
		return G.multiply(element);
	}
	
	public ECPoint BigIntegerToPoint(BigInteger element) {//BigInteger转为Point
		return G.multiply(element);
	}
	
	public ECPoint encryptPoint(ECPoint point) {//使用自己的密钥加密元素对应的点
		return point.multiply(key);
	}
	
	public ECPoint decryptPoint(ECPoint point) {//使用自己的密钥的逆元解密
		return point.multiply(key_inv);
	}
	
	public ECPoint bytesToECPoint(byte[] bytes) {
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(Params.eccEnum.getEccName());
		ECPoint point = ecSpec.getCurve().decodePoint(bytes);
		return point;
	}
	
}
