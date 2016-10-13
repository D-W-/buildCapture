package com.dw;

import java.io.File;

public class ProcessMakefile {
	
	public static void main(String[] args) {
//		这是程序的入口,主要接收参数,并调用其他的类来完成操作
//		第一个参数是make文件所在目录
		String makeFolder = "";
		makeFolder = args[0];
		
		if(!makeFolder.equals("")){
//			先处理一下用户目录的问题 ~无法识别
			if(makeFolder.charAt(0) == '~'){
				makeFolder = System.getProperty("user.home") + makeFolder.substring(1);
			}
			String command = "";
			for(int i = 1;i<args.length;++i){
				command += " ";
				command += args[i];
			}
			
			String outFolder = makeFolder;
			ParameterHandler parameterHandler = new ParameterHandler(outFolder);
			command = parameterHandler.extract(command);
			outFolder = parameterHandler.getOutput();
			if(outFolder.charAt(0) == '~'){
				outFolder = System.getProperty("user.home") + outFolder.substring(1);
			}
			command = "cd " + makeFolder + " && " + command;
			outFolder += "/.process_makefile";
			File rootFolder = new File(outFolder);
			rootFolder.mkdir();
			parameterHandler.make(command);
			
//			输入一个 output 文件,执行输出增加 -E 选项
			GetDetectedTasks getDetectTasks = new GetDetectedTasks(makeFolder,outFolder);
			getDetectTasks.deal();
		}
	}
}
