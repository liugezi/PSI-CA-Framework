import java.util.Random;

public class Shuffle {

	public static void main(String[] args) {
        String[] datas = new String[1000000];
        
        for(int i = 0; i < 1000000; ++i) {
        	datas[i] = i + "123232312";
        }
        long startTime = System.currentTimeMillis();
        String[] returnDatas = shuffle(datas);
        long endTime = System.currentTimeMillis();
        System.out.println("100w¸öÔªËØÏ´ÅÆ²Ù×÷µÄºÄÊ±:" + (endTime - startTime) / 1000 + "Ãë" + (endTime - startTime) % 1000 + "ºÁÃë");
        System.out.println("nums:"+ returnDatas.length);
//        for(int i = 0, length = returnDatas.length; i < length; i++) {
//            System.out.print(returnDatas[i]+", ");
//        }
    }
    
    public static String[] shuffle(String[] nums) {
        Random rnd = new Random();
        for(int i = nums.length-1; i > 0; i-- ) {
            int j = rnd.nextInt(i+1);
            String temp = nums[i];
            nums[i] = nums[j];
            nums[j] = temp;
        }
        return nums;
    }

}
