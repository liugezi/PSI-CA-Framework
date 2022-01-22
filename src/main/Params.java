import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import com.google.common.collect.testing.TestMapEntrySetGenerator;

public class Params {
	public enum ProtocolEnum {//用于选择不同的协议
		Unbalanced, Reversed, ECC_Unbalanced, ECC_Reversed
	}
	
	public enum FilterEnum {
		BloomFilter, CuckooFilter
	}
	
	public enum StageEnum {
		Prefix_filter, Prefix_bloom, Encrypt, Enc_bloom, Enc_cuckoo
	}
	
	public static final int THREADS = 8;//控制协议的并发度
	
	public static final boolean pirFilter = false;
	
	public static final boolean preFilter = true;
	
	public static final int client_size = (int)Math.pow(2, 18);//客户端数据集大小
	public static final int server_size = (int)Math.pow(2, 15);//服务端数据集大小
	
	public static final int prefix_len = 16; //哈希预过滤的前缀长度
	
	public static final ProtocolEnum protocol = ProtocolEnum.ECC_Reversed; //用于决定执行哪个协议
	
	public static final ECCEnum eccEnum = ECCEnum.SM2; //用于决定使用哪个椭圆曲线
	
	public static final FilterEnum filterEnum = FilterEnum.CuckooFilter; //用于决定使用哪个过滤器
	
	public static final BigInteger pub_random = new BigInteger("38520159238394498553641278910"); //用于保证安全性的公开随机参数
	
}
