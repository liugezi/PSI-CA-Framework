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
	//ϣ��������ܷ�װ��ͬ����Բ���߼ӽ����㷨, ��ʼ��ֻ��ָ������������
	public static void main(String[] args) {
			EccEnc eccEnc = new EccEnc();
			eccEnc.generateKey();
			String telString = "13382958827";//����������
			BigInteger I = new BigInteger(telString);
			ECPoint encPoint = eccEnc.G.multiply(I);//��ѹ����ʽ����
			
			byte[] result = encPoint.getEncoded(false);
			
			System.out.println("����ǰ�ĵ�: " + new String(result));
			
			encPoint = encPoint.multiply(eccEnc.key);
			result = encPoint.getEncoded(false);
			
			System.out.println("һ�μ��ܺ�ĵ�: " + new String(result));
			
			encPoint = eccEnc.bytesToECPoint(result);//�Ƚ�byte[]��ʽ����תΪ��Բ�����ϵĵ�
			
			//encPoint = encPoint.multiply(eccEnc.key2);//���μ���
			
			result = encPoint.getEncoded(true);
			
			System.out.println("���μ��ܺ�ĵ�: " + new String(result));
			
			//ECPoint encPoint2 = eccEnc.bytesToECPoint(result);
			//encPoint = encPoint.multiply(eccEnc.key2_inv);
			
			result = encPoint.getEncoded(true);
			
			System.out.println("һ�ν��ܺ�ĵ�: " + new String(result));
			
			String tempString = new String(result);
			
			byte[] result2 = tempString.getBytes(); 
			
			System.out.println("����string�ָ�������byte����Բ���: " + new String(result2));
			
			encPoint = encPoint.multiply(eccEnc.key_inv);
			result = encPoint.getEncoded(true);
			
			System.out.println("2�ν��ܺ�ĵ�: " + new String(result));
			
			String hexOutputString = Utils.bytesToHexString(result);
			
			System.out.println("2�ν��ܺ�ĵ�(16����): " + hexOutputString);
			
			result = Utils.hexStringToBytes(hexOutputString);
			
			System.out.println("2�ν��ܺ�ĵ�: " + new String(result));
			
			//Vector<String> vector = ECNamedCurveTable.getNames();
			
			Enumeration<String> enumeration = ECNamedCurveTable.getNames();
			System.out.println("�鿴���õ���Բ�����б�:");
			int cnt = 1;
			while(enumeration.hasMoreElements()) {
				System.out.println("��" + cnt++ + "��:" + enumeration.nextElement());
			}
			
	}
	
	public EccEnc() {//���캯��
		generateKey();
	}
	
	//�Ȼ���BouncyCastleʵ��һ���򵥵�ECC
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
	
	public ECPoint hashToPoint(byte[] hash) {//��Ԫ�صĹ�ϣֵתΪPoint
		BigInteger element = new BigInteger(hash);
		return G.multiply(element);
	}
	
	public ECPoint BigIntegerToPoint(BigInteger element) {//BigIntegerתΪPoint
		return G.multiply(element);
	}
	
	public ECPoint encryptPoint(ECPoint point) {//ʹ���Լ�����Կ����Ԫ�ض�Ӧ�ĵ�
		return point.multiply(key);
	}
	
	public ECPoint decryptPoint(ECPoint point) {//ʹ���Լ�����Կ����Ԫ����
		return point.multiply(key_inv);
	}
	
	public ECPoint bytesToECPoint(byte[] bytes) {
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(Params.eccEnum.getEccName());
		ECPoint point = ecSpec.getCurve().decodePoint(bytes);
		return point;
	}
	
}
