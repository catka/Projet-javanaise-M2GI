package utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import irc.Irc;

public class BurstTests {
	
	
	public static void main(String[] args) {
		System.out.println("TEST");
		
		
		
		for(int i=0; i < 2; i++) {
			generateBurst(i);
		}
		//Irc.main(null);
	}
	
	
	public static int exec(Class mClass, 
						List<String> jvmArgs, 
						List<String> args) throws IOException, InterruptedException{
		
		String javaHome = System.getProperty("java.home");
		String javaBin = javaBin = javaHome + File.separator + "bin" + File.separator + "java";
		String classPath = System.getProperty("java.class.path");
		String className = mClass.getName(); 
		
		List<String> command = new ArrayList<String>();
		command.add(javaBin);
		if(jvmArgs != null)command.addAll(jvmArgs);
		command.add("-cp");
		command.add(classPath);
		command.add(className);
		if(args != null)command.addAll(args);
		
		ProcessBuilder builder = new ProcessBuilder(command);
		Process process = builder.inheritIO().start();
		process.waitFor();
		return process.exitValue();
		
	}
	
	
	private static void generateBurst(int prgIndex) {
		Thread t = new Thread() {
			public void run() {
				System.out.println("Test : " + prgIndex);
				
				List<String> args = new ArrayList<String>();
				args.add(String.valueOf(prgIndex));
				
				try {
					int res = exec(Irc.class, null, args);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}
	
	
	

}
