package com.dw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.util.Execute;

public class ParameterHandler {
	/*
	 * 处理输入参数,接收输入参数,处理参数,对合适的指令调用make指令
	 * 输出重定向到一个make输出文件
	 * 假设输出给我的(不用make处理的)参数 是这样的形式 -mk*=*
	 * 指定输出目录的参数 -mko=folder
	 * 指定path -path=path
	 */
	private static String pattern = "-mk[^=]+=[^ ]+";
	private static String pathPattern = "-path=[^ ]+";
//	需要此工具处理的参数都放到这里面
	private static HashMap<String,String> arguments;
	private String folderName;
	
	public ParameterHandler(String folderString) {
		arguments = new HashMap<String,String>();
		folderName = folderString;
	}
	
	public String extract(String command){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(command);
		while(m.find()){
			String[] store = m.group(0).split("=");
			arguments.put(store[0], store[1]);
//			System.out.println(m.group(0));
		}
		command = m.replaceAll("");
		p = Pattern.compile(pathPattern);
		m = p.matcher(command);
		List<String> pathsStore = new ArrayList<>();
		while(m.find()){
			String[] store = m.group(0).split("=");
			arguments.put(store[0], store[1]);
			pathsStore.add(store[1]);
		}
		Execute.paths = new String[pathsStore.size()];
		Execute.paths = pathsStore.toArray(Execute.paths);
		command = m.replaceAll("");
//		System.out.println(command);
		return command;
	}
	
	public boolean make(String command){
//		先进行用户要求的make,并将输出存到 output 这个文件里面,以供后续处理
//		做了更改,原来是直接执行一个语句,但是 TO 项目无法退出(shell 下面使可以退出的)
//		现在放到一个脚本里面执行 可以了
		command += (" > " + folderName + "/.process_makefile/output");
		String[] commands = {command};
		return Execute.executeCommands(commands);
	}
	
	public boolean makewithExpand(String command){
//		这个方法是当正常make抓取不到时, 尝试使shell在执行语句之前先输出expanding的语句的操作
//		但是	1.需要抓取标准错误
//			2.需要去掉每行前面的加号

//		抓取标准错误
		command += (" \"SHELL=sh -xv\" -f Makefile >" + folderName + "/.process_makefile/output 2>&1");
		String[] commands = {command};
		boolean result = Execute.executeCommands(commands);
//		去掉每行之前的加号
		parseOutput();
		return result;
	}
	
	private void parseOutput(){
//		去掉每行之前的加号
		String outputfileName = folderName + "/.process_makefile/output";
		String tmpfileName = outputfileName + ".tmp";
		BufferedReader br = null;
	    BufferedWriter bw = null;
	      try {
	         br = new BufferedReader(new FileReader(outputfileName));
	         bw = new BufferedWriter(new FileWriter(tmpfileName));
	         String line;
	         while ((line = br.readLine()) != null) {
	            if (line.length() > 0 && line.charAt(0) == '+')
	               line = line.substring(1);
//	            TODO: line processing
	            bw.write(line+"\n");
	         }
	      } catch (Exception e) {
	         return;
	      } finally {
	         try {
	            if(br != null)
	               br.close();
	         } catch (IOException e) {
	            //
	         }
	         try {
	            if(bw != null)
	               bw.close();
	         } catch (IOException e) {
	            //
	         }
	      }
	      // Once everything is complete, delete old file..
	      File oldFile = new File(outputfileName);
	      oldFile.delete();

	      // And rename tmp file's name to old file name
	      File newFile = new File(tmpfileName);
	      newFile.renameTo(oldFile);
	}
	
	public String getOutput(){
//		查看用户是否指定输出,没有指定就返回 null
		if(arguments.containsKey("-mko")){
			this.folderName = arguments.get("-mko");
		} 
		return this.folderName;
	}
	
	public static void mainTest(String[] args) {
		String command = "";
		for(int i = 0;i<args.length;++i){
			command += " ";
			command += args[i];
		}
		ParameterHandler outerShell = new ParameterHandler(".process_makefile");
		command = outerShell.extract(command);
		outerShell.make(command);
	}
}
