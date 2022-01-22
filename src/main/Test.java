import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import com.google.common.primitives.Bytes;

public class Test {

//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		Keys key1 = new Keys(new BigInteger("1234"), new BigInteger("5678"), new BigInteger("91011"));
//		Keys key2 = new Keys(new BigInteger("1234"), new BigInteger("5678"), new BigInteger("91011"));
//		Set<Keys> keysSet = new HashSet<Keys>();
//		keysSet.add(key1);
//		keysSet.add(key2);
//		System.out.println(key1.equals(key2));
//		System.out.println(key1.hashCode() == key2.hashCode());
//        System.out.println(keysSet);
//	}
//	 public static void main(String[] args) {
//		 long startTime = System.currentTimeMillis(); //获取开始时间
// 		 long endTime;
//	    	try {
//	    		File file = new File("client/clientDB3.txt");
//	    		if(file.exists()){
//	    			long fileLength = file.length();
//	    			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
//	    			lineNumberReader.skip(fileLength);
//	                int lines = lineNumberReader.getLineNumber();
//	                System.out.println("Total number of lines : " + lines);
//	                endTime = System.currentTimeMillis();
//	                System.out.println("先获取file.length()再skip耗费时间：" + (endTime-startTime)/1000 + "s" +
//	       				 (endTime-startTime) % 1000 +"ms");
//	                lineNumberReader.close();
//	    		}else {
//	    			System.out.println("File does not exists!");
//	    		}
//	    		
//	    		startTime = System.currentTimeMillis();
//	    		File file2 = new File("client/clientDB3.txt");
//	    		if(file2.exists()){
//	    			//long fileLength = file2.length();
//	    			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file2));
//	    			lineNumberReader.skip(Long.MAX_VALUE);
//	                int lines = lineNumberReader.getLineNumber();
//	                System.out.println("Total number of lines : " + lines);
//	                endTime = System.currentTimeMillis();
//	                System.out.println("直接skip Long.MAX_VALUE耗费时间：" + (endTime-startTime)/1000 + "s" +
//	       				 (endTime-startTime) % 1000 +"ms");
//	                lineNumberReader.close();
//	    		}else {
//	    			System.out.println("File does not exists!");
//	    		}
//	    		
//	    	}catch(IOException e) {
//	    		e.printStackTrace();
//	    	}
//	    }
	 
	 public static void main(String[] args) {
		 String s1 = "北京市海淀区学院路37号123123北京市海淀区学院路37号北京市海淀区学院路37号";
		 try {
			byte[] bytes = s1.getBytes("utf-8");
			String s3 = new String(bytes);
			System.out.println("明文:" + s1);
			System.out.println("明文转为byte数组,用byte数组初始化String:" + s3);
			String decryptData2 = new String(s3.getBytes(), "utf-8");
			System.out.println("将String通过utf-8解码转成明文:" + decryptData2);
			
			
			String s2 = bytes.toString();
			String decryptData1 = new String(s2.getBytes(), "utf-8");
			System.out.println("明文转为utf-8的byte数组再用toString()转为String:" + s2);
			System.out.println("byte数组转为String:" + decryptData1);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }

}
