package com.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

public class Execute {
/*
 * 这个类是专门用来执行某条指令的
 */
	public static String[] paths = null;
	
	public static void executeCommand(String command){
//		执行一条linux命令 如果命令执行不成功 就直接产生一个异常
		Runtime runtime = Runtime.getRuntime();
		try {
			Process process = runtime.exec(new String[] {"/bin/sh","-c",command});
			int waitID = process.waitFor();
			if(waitID != 0){
				InputStreamReader isr = new InputStreamReader(process.getErrorStream());
				BufferedReader br = new BufferedReader(isr);
				String buffer = "";
				String line = null;
				while((line = br.readLine()) != null){
					buffer += line;
				}
				br.close();
				isr.close();
				throw new InterruptedException("shell failed!\n" + buffer);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}	
	}
	
	public static boolean executeCommands(String[] commands) {
//		创建一个临时的 bash 文件来执行,可以执行 extglob 的语句 也可以执行多条语句
		boolean result = true;
		File tempScript = null;
	    try {
	    	tempScript = createTempScript(commands);
	        ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
	        if(paths != null){
//	        	加入 PATH 环境变量
	            String envPath = pb.environment().get("PATH");
	            for(String path : paths){
	                envPath += (":" + path);
	            }
	            pb.environment().put("PATH", envPath);
	        }
	        pb.inheritIO();
	        Process process = pb.start();
	        int waitID = process.waitFor();
			if(waitID != 0){
				InputStreamReader isr = new InputStreamReader(process.getErrorStream());
				BufferedReader br = new BufferedReader(isr);
				String buffer = "";
				String line = null;
				while((line = br.readLine()) != null){
					buffer += line;
				}
				br.close();
				isr.close();
				throw new InterruptedException("shell failed!\n" + buffer);
			}
	    } catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		} finally {
	        tempScript.delete();
	    }
	    return result;
	}
	
	public static File createTempScript(String[] commands) throws IOException {
//		创建文件
	    File tempScript = File.createTempFile("processMakefileScript", null);

	    Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
	            tempScript));
	    PrintWriter printWriter = new PrintWriter(streamWriter);

	    printWriter.println("#!/bin/bash");
	    for(int i = 0;i < commands.length;++i){
	    	printWriter.println(commands[i]);
	    }
	    printWriter.flush();
	    printWriter.close();

	    return tempScript;
	}
}
