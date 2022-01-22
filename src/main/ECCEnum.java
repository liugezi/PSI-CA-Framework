
public enum ECCEnum {
	
	SM2("sm2p256v1"),
	K283("K-283"),
	P256("P-256"),
	secp256k1("secp256k1"),
	secp224r1("secp224r1");
	
	private final String eccName;

	ECCEnum(String eccName) {
		this.eccName = eccName;
	}
	
	public String getEccName() {
		return eccName;
	}
	
	@Override
	public String toString() {
		return "ECC [ECCName=" + eccName + "]";
	}
	
	public static void main(String[] args) {
		ECCEnum eccEnum = ECCEnum.SM2;
		System.out.println(eccEnum);
		System.out.println(eccEnum.getEccName());
	}
}
