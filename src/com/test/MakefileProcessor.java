package com.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import com.dw.GetDetectedTasks;
import com.dw.ParameterHandler;
import com.util.Execute;

import cn.harry.captor.Tasks;

public class MakefileProcessor{

	public static String _makeFolder = null;
	public static String _outFolder = null;
	public static Integer taskNumber = 0;
	public static Tasks taskList;
	
	public static boolean runWithPath(String makeFolder, String outFolder, String path){
//		增加环境变量path
		ArrayList<String> items = new ArrayList<String>();
//		从文件中读取
		File file = new File(path);
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String temp;
			while((temp = br.readLine()) != null){
				items.add(temp);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Read file error!");
		}
//		最后需要转换成 array
		Execute.paths = new String[items.size()];
		Execute.paths = items.toArray(Execute.paths);
		return run(makeFolder, outFolder);
	}
	
	public static boolean run(String makeFolder, String outFolder){
//		调用 Makefile Processor 函数, 需要输入工程路径和输出路径
		if(outFolder == null){
			outFolder = makeFolder;
		}
		if(!makeFolder.equals("")){
//			先处理一下用户目录的问题 ~无法识别
			if(makeFolder.charAt(0) == '~'){
				makeFolder = System.getProperty("user.home") + makeFolder.substring(1);
			}
			if(outFolder.charAt(0) == '~'){
				outFolder = System.getProperty("user.home") + outFolder.substring(1);
			}
			_makeFolder = makeFolder;
			_outFolder = outFolder;
			
			String command = "cd " + makeFolder + " && make";
			ParameterHandler parameterHandler = new ParameterHandler(makeFolder, outFolder);
			outFolder += "/.process_makefile";
			File rootFolder = new File(outFolder);
			rootFolder.mkdir();
			parameterHandler.make(command);
			
//			输入一个 output 文件,执行输出增加 -E 选项
			GetDetectedTasks getDetectTasks = new GetDetectedTasks(makeFolder,outFolder);
			getDetectTasks.deal();
//			taskNumber = getDetectTasks.getTaskNumber();
			taskList = getDetectTasks.getTaskList();
			return true;
		}
		return false;
	}
	
	public static void makeClean(){
//		make clean, 清除所有的 .o 文件, 但是需要先执行 run() 才能调用
		if(_makeFolder != null)
			Execute.executeCommand("cd " + _makeFolder + " && make clean");
	}

	public static boolean runWithPathCleanO(String makeFolder, String outFolder, String path){
//		带有路径地 run 完 clean 所有的 .o 文件
		boolean result = runWithPath(makeFolder, outFolder, path);
		if(result){
			makeClean();
		}
		return result;
	}
	
	public static boolean runCleanO(String makeFolder, String outFolder){
//		run 完之后 clean 所有的 .o 文件
		boolean result = run(makeFolder, outFolder);
		if(result){
			makeClean();
		}
		return result;
	}
	
	public static Tasks getTaskPaths(){
//		hash<String, List<String>>
//		得到生成的所有的task路径, 如果当前的工程没有生成可执行文件,就返回一个空列表
		return taskList;
	}
	
	public static void clean(){
//		执行make clean 然后删除 ProcessLog
		if(_makeFolder != null && _outFolder != null){
//			必须执行过run() 之后才能clean
			Execute.executeCommand("cd " + _makeFolder + " && make clean");
			Execute.executeCommand("cd "+ _outFolder + " && rm -r .process_makefile");
		}
	}
	
	public static void mainTest(String[] args){
		runWithPath("/home/harry/code/TO_CORE", null, "/home/harry/code/java/SWTWindow/path.txt");
		System.out.println(getTaskPaths());
		clean();
	}
}
