import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

//import org.omg.CORBA.PRIVATE_MEMBER;
//import org.omg.CORBA.PUBLIC_MEMBER;

public class DB {

	public static void main(String[] args) {
		//生成随机操作指令码op
		SecureRandom ran;
		String nameString;
		
		try {
			nameString = "server/serverDB" + Params.server_size + ".txt";
			File file = new File(nameString);
			if(!file.exists()) {
				for(int i = 0; i < Params.server_size; ++i) {//生成小集合serverDB
					ran = SecureRandom.getInstance("SHA1PRNG");
					//nameString = "server/serverDB" + Params.server_size + ".txt";
					CommEnc.writeTxt(nameString, createMobile(ran.nextInt(3)) + "\n");
					if(i % 1000 == 0) {
						System.out.println("serverDB第" + i + "个手机号已生成");
					}
				}
			}
			
			nameString = "client/clientDB" + Params.client_size + ".txt";
			file = new File(nameString);
			if(!file.exists()) {
				for(int i = 0; i < Params.client_size; ++i) {//生成大集合clientDB
					ran = SecureRandom.getInstance("SHA1PRNG");
					
					CommEnc.writeTxt(nameString, createMobile(ran.nextInt(3)) + "\n");
					if(i % 5000 == 0) {
						System.out.println("clientDB第" + i + "个手机号已生成");
					}
				}
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}//生成随机数决定采用哪一个素数p
	}
	
	public static void generateServerDB() {
		String nameString;
		SecureRandom ran;
		nameString = "server/serverDB" + Params.server_size + ".txt";
		File file = new File(nameString);
		try {
		if(!file.exists()) {
			for(int i = 0; i < Params.server_size; ++i) {//生成小集合serverDB
				
					ran = SecureRandom.getInstance("SHA1PRNG");
				//nameString = "server/serverDB" + Params.server_size + ".txt";
				CommEnc.writeTxt(nameString, createMobile(ran.nextInt(3)) + "\n");
				if(i % 1000 == 0) {
					System.out.println("serverDB第" + i + "个手机号已生成");
				}
			}
		} } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public static void generateClientDB() {
		String nameString;
		SecureRandom ran;
		nameString = "client/clientDB" + Params.client_size + ".txt";
		File file = new File(nameString);
		try {
			if(!file.exists()) {
				for(int i = 0; i < Params.client_size; ++i) {//生成大集合clientDB
					ran = SecureRandom.getInstance("SHA1PRNG");
					CommEnc.writeTxt(nameString, createMobile(ran.nextInt(3)) + "\n");
					if(i % 5000 == 0) {
						System.out.println("clientDB第" + i + "个手机号已生成");
					}
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
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
