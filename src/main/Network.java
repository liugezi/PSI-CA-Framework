import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

//import javax.xml.bind.DataBindingException;


public class Network {
	protected Socket sock;
	protected ServerSocket serverSock;
	public InputStream is;
	public OutputStream os;
	public DataInputStream d_in;
	public DataOutputStream d_out;
	
	public Network() {
	}
	
	public Network(InputStream is, OutputStream os, Socket sock) {//构造函数,用Network对象管理发送和接收过程
		this.is = is;
		this.os = os;
		this.sock = sock;
	}
	
	public void sendFile(String filePath) {//过长、过多的密文信息直接发送文件
		File file = new File(filePath);
        try {
        	d_out = new DataOutputStream(sock.getOutputStream());
        	//先获取文件长度并发送给对方
        	long length = file.length();
        	//System.out.println("Send file length: " + length);
        	d_out.writeLong(length);
            FileInputStream f_in = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = (f_in.read(buffer))) > 0) {
                d_out.write(buffer, 0, read);
            }
            d_out.flush();
            f_in.close();
            //d_out.write("\n");
            //d_out.close();//没有这行代码会出Bug，使得文件无法接收
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	//在多线程的情况下，将拆分后的多个文件逐个发送给服务端，传入threads用于访问这些文件
	public void sendFiles(int threads) {//过长、过多的密文信息直接发送文件
		File file;
		for(int i = 1; i <= threads; ++i) {
			file = new File("client/" + i + "out");
			try {
	        	d_out = new DataOutputStream(sock.getOutputStream());
	            FileInputStream f_in = new FileInputStream(file);
	            int all = 0;
	            byte[] buffer = new byte[1024];
	            int read = 0;
	            while ((read = (f_in.read(buffer))) > 0) {
	                d_out.write(buffer, 0, read);
	                all += read;
	            }
	            System.out.println("Send file length: "+all);
	            d_out.flush();
	            f_in.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		//File file = new File(filePath);    
    }
	
	public void receiveFile(String filePath) {//接收文件
		try {
			long startTime; //获取开始时间
			long endTime;
			byte[] buf = new byte[1024];
			int len = 0;
			//System.out.println("开始接收文件！");
			startTime = System.currentTimeMillis();
			d_in = new DataInputStream(sock.getInputStream());
			//先获取文件长度，再开始接收内容
			long length = d_in.readLong();//文件总长度
			//System.out.println("文件长度为:" + length);
			long group = length % 1024 == 0 ? length / 1024 : length / 1024 + 1;//文件分组
			long cnt = 0;//计数器
			DataOutputStream dosOutputStream = new DataOutputStream(new FileOutputStream(filePath));
			while((len = d_in.read(buf)) != -1) {//使用这种阻塞方式如果另一方 
				dosOutputStream.write(buf, 0, len);
				cnt++;
				if(cnt >= group) break;
			}
			dosOutputStream.flush();
			endTime = System.currentTimeMillis();
			System.out.println("程序运行时间： "+(endTime-startTime)/1000 + "s" +
					 (endTime-startTime) % 1000 +"ms");
			System.out.println("文件接收结束!");
			//d_in.close();
			dosOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//sendBytes:发送byte数组
	public void sendBytes(byte[] data) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            out.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//sendBI:对于BigInteger的情况进行封装
	public void sendBI(BigInteger bi) {
		System.out.println("素数P:"+bi.toString());
		sendBytes(Utils.bigIntegerToBytes(bi));
	}
	
	//sendStr:对于String变量的情况进行封装
	public void sendStr(String str) {//对于String类型变量需要转成utf-8编码再发送
		try {
			byte[] block = str.getBytes("utf-8");
			sendBytes(block);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	//readBytes:读取内容
	public byte[] readBytes(int len) {//对给定长度的内容进行读取
		byte[] temp = new byte[len];
		try {
			int remain = len;
			int readBytes;//实际读取的byte个数
			readBytes = is.read(temp, len - remain, remain);//len-remain计算的是读数据的起始点
			System.out.println(readBytes);
			if (readBytes != -1) {
				remain -= readBytes;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return temp;
	}
	
	
	public void readBytes(byte[] temp) {
		try {
			int remain = temp.length;
			while (0 < remain) {
				int readBytes;
				readBytes = is.read(temp, temp.length - remain, remain);
				if (readBytes != -1) {
					remain -= readBytes;
				}  
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] readBytes() {
		byte[] bytes = null;
        try {
            ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
            bytes = (byte[])in.readObject();
            System.out.println("Receive bytes[] length: "+bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return bytes;
	}
	
	public void flush() {//刷新输出流
		try {
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {//断开连接
		try {
			if(sock != null) {
				sock.close();
			}
			if(serverSock != null){
				serverSock.close();
			}
			if(d_in != null) {
				d_in.close();
			}
			if(d_out != null) {
				d_out.close();
			}
			if(is != null) {
				is.close();
			}
			if(os != null) {
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
