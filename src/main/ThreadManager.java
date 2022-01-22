
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.apache.log4j.chainsaw.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.Test2")
public class ThreadManager {
	protected static Logger logger = LoggerFactory.getLogger(ThreadManager.class);
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		Thread t1 = new Thread() {
			@Override
			public void run() {
				method1(20);
			}
		};
		t1.setName("t1");
		t1.start();
		method1(10);
	}
	private static void method1(int x) {
		int y = x + 1;
		Object mObject = method2();
		System.out.println(mObject);
	}
	
	private static Object method2() {
		Object nObject = new Object();
		return nObject;
	}
}
//Runnable runnable = new Runnable() {//任务是任务，线程是线程
//@Override
//public void run() {
//	// 要执行的任务
//	logger.debug("running");
//}
//};
////创建线程对象
//Thread t1 = new Thread(runnable, "t1");
////启动线程
//t1.start();
//
////创建任务对象
//Runnable task2 = () -> {logger.debug("hello");};
////参数1 是任务对象； 参数2 是线程名字，推荐
//Thread t2 = new Thread(task2, "t2");
//t2.start();
//
//Runnable r = () -> {
//	// 要执行的任务
//	logger.debug("running");
//	logger.debug("running2");
//	logger.debug("running3");
//};
//Thread t3 = new Thread(r, "t3");
//t3.start();
//
////任务对象
//FutureTask<Integer> task = new FutureTask<Integer>(new Callable<Integer>() {
//@Override
//public Integer call() throws Exception {
//	logger.debug("running...");
//	Thread.sleep(2000);
//	return 100;
//}
//});
//
////借助Thread执行
//Thread thread = new Thread(task,"t4");
//thread.start();
//logger.debug("{}", task.get());//"{}"表示占位符

//new Thread(() -> {
//while(true) {
//	logger.debug("running");
//}
//}, "t1").start();
//
//new Thread(() -> { 
//while(true) {
//	logger.debug("running");
//}
//}, "t2").start();
