import java.math.BigInteger;

import lombok.ToString;

public class Keys {
	private BigInteger p;// 选定素数p
	private BigInteger a, a_inv;

	public Keys(BigInteger p) {
		this.p = p;
	}
	
	public Keys(BigInteger p, BigInteger a, BigInteger a_inv) {
		this.p = p;
		this.a = a;
		this.a_inv = a_inv;
	}

	public void setA(BigInteger a) {
		this.a = a;
	}

	public void setA_Inv(BigInteger a_inv) {
		this.a_inv = a_inv;
	}
	
	public BigInteger getA() {
		return a;
	}
	
	public BigInteger getAInv() {
		return a_inv;
	}
	
	public BigInteger getP() {
		return p;
	}
	
	@Override
	public String toString() {
		String string = "p:" + p.toString() + "\n"
				+ "a:" + a.toString() + "\n"
				+ "a_inv:" + a_inv.toString() + "\n";
		return string;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) 
			return true;//如果地址相等，那可以直接返回
		
        if(obj == null){
            return false;//非空性：对于任意非空引用x，x.equals(null)应该返回false。
        }

        if(obj instanceof Keys){
            Keys other = (Keys) obj;
            //需要比较的字段相等，则这两个对象相等
            if(this.a.compareTo(other.getA()) == 0 &&
               this.a_inv.compareTo(other.getAInv()) == 0 &&
               this.p.compareTo(other.getP()) == 0)
                return true; 
        }

        return false;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
        result = 31 * result + (a == null ? 0 : a.hashCode());
        result = 31 * result + (a_inv == null ? 0 : a_inv.hashCode());
        result = 31 * result + (p == null ? 0 : p.hashCode());
        return result;
	}
}
