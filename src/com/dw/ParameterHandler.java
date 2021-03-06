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

import cn.harry.captor.MakefileCapture;

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
	private String outputfolder;
	private String inputfolder;
	
	public ParameterHandler(String inputFolder, String outputFolder) {
		arguments = new HashMap<String,String>();
		this.outputfolder = outputFolder;
		this.inputfolder = inputFolder;
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
	
	/*
	 * @command: 用户的输入命令(在这里应该就是cd 和 make), 后续可以改一下接口
	 * 先进行用户要求的make,并将输出存到 output 这个文件里面,以供后续处理
		修改1: 做了更改,原来是直接执行一个语句,但是 TO 项目无法退出(shell 下面使可以退出的)
		现在放到一个脚本里面执行 可以了
		修改2: 当正常make抓取不到时, 尝试使shell在执行语句之前先输出expanding的语句的操作
		原来的输出存储不变, 但是增加了抓取不到的时候读的文件 /.process_makefile/all
		修改3: 增加选项, 可以指定脚本
	 */
	public boolean make(String command, String shell){
		if(shell == null || shell.equals("")) {
			shell = MakefileCapture.SHELL;
		}
		File tempFile = new File(inputfolder + "/Makefile");
//		判断makefile or Makefile
		if(tempFile.exists()){
//			command = command + (" \"SHELL=" + shell + " -xv\" -f Makefile &> " + outputfolder + "/.process_makefile/all ");
			command = command + (" >" + outputfolder + "/.process_makefile/output ");
		} else {
//			command = command + (" \"SHELL=" + shell + " -xv\" -f makefile &> " + outputfolder + "/.process_makefile/all " );
      command = command + (" >" + outputfolder + "/.process_makefile/output ");
		}
		String[] commands = {command};
		boolean result = Execute.executeCommands(commands);
		if(result) {
//			filterCapturedFiles();
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean make(String command) {
		return make(command, MakefileCapture.SHELL);
	}
	
/*
 * 	对抓取到的文件进行过滤
 *      1.需要去掉每行前面的加号
 *      2.需要去掉无用的行(空行 #开头的行)
 */
	public void filterCapturedFiles() {
//		rename all to output and store output to output.old
		String outputfileName = outputfolder + "/.process_makefile/output";
		
		File outputfile = new File(outputfileName);
		try {
			outputfile.createNewFile();
		} catch (IOException e1) {
			throw new RuntimeException(e1.toString());
		}
		
		String allName = outputfolder + "/.process_makefile/all";
		
		Pattern startWithPlus = Pattern.compile("^\\+* ");
		Pattern pattern_startWithArCC = Pattern.compile("^\\s*([/a-z0-9-_]*-)?(g?cc|ar) ");
		
		BufferedReader br = null;
	    BufferedWriter bw = null;
	      try {
	         br = new BufferedReader(new FileReader(allName));
	         bw = new BufferedWriter(new FileWriter(outputfileName));
	         String line;
//	         consider all gcc lines, may contains duplicate
	         while ((line = br.readLine()) != null) {
	            if (line.length() > 0) {
					Matcher matcher = pattern_startWithArCC.matcher(line);
	            	if (line.startsWith("make")) {
						bw.write(line+"\n");
					}
					else if (matcher.find()) {
	            		bw.write(line + "\n");
					} else {
						matcher = startWithPlus.matcher(line);
						if(matcher.find()) {
//		        		去掉每行之前的加号
							line = matcher.replaceAll("");
							matcher = pattern_startWithArCC.matcher(line);
							if (matcher.find()) {
								bw.write(line + "\n");
							}
						}
					}
	            }
	         }
	      } catch (Exception e) {
	         return;
	      } finally {
	         try {
	            if(br != null)
	               br.close();
	         } catch (IOException e) {
	            throw new RuntimeException(e.toString());
	         }
	         try {
	            if(bw != null)
	               bw.close();
	         } catch (IOException e) {
	        	 throw new RuntimeException(e.toString());
	         }
	      }
	}
	
	public String getOutput(){
//		查看用户是否指定输出,没有指定就返回 null
		if(arguments.containsKey("-mko")){
			this.outputfolder = arguments.get("-mko");
		} 
		return this.outputfolder;
	}
	
	public static void main(String[] args) {
//		String command = "";
//		for(int i = 0;i<args.length;++i){
//			command += " ";
//			command += args[i];
//		}
//		ParameterHandler outerShell = new ParameterHandler(".process_makefile");
//		command = outerShell.extract(command);
//		outerShell.make(command);
		ParameterHandler parameterHandler = new ParameterHandler("/home/harry/Downloads/openssl-1.0.0a/", "/home/harry/Downloads/openssl-1.0.0a/");
		parameterHandler.filterCapturedFiles();

	}
}
