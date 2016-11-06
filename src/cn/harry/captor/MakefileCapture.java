package cn.harry.captor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.dw.GetDetectedTasks;
import com.dw.ParameterHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.util.Execute;

public class MakefileCapture {
	/*
	 * 提供特定的接口以调用 makefile capture
	 */
	private  String _projectDirectory = null;
	private String _outputDirectory = null;
	
//	public static HashMap<String, List<String>> taskMap;
	public static List<List<String>> tasks;
	
	public MakefileCapture(String projectDirectory, String outputDirectory){
		_projectDirectory = projectDirectory;
		_outputDirectory = outputDirectory;
	}
	
	public MakefileCapture(String projectDirectory){
		_projectDirectory = projectDirectory;
		_outputDirectory = projectDirectory;
	}
	
	public boolean make(String makeCommand){
//    编译并抓取
		if(_projectDirectory != null && !_projectDirectory.equals("")){
			String makeFolder = _projectDirectory;
			String outFolder = _outputDirectory;
			
//			先处理一下用户目录的问题 ~无法识别
			if(makeFolder.charAt(0) == '~'){
				makeFolder = System.getProperty("user.home") + makeFolder.substring(1);
			}
			if(outFolder.charAt(0) == '~'){
				outFolder = System.getProperty("user.home") + outFolder.substring(1);
			}
			
			if(makeCommand == null || makeCommand.equals("")) {
				makeCommand = "make";
			}
//			对工程执行make，并抓取输出
			String command = "cd " + makeFolder + " && " + makeCommand;
			ParameterHandler parameterHandler = new ParameterHandler(outFolder);
			outFolder += "/.process_makefile";
			File rootFolder = new File(outFolder);
			rootFolder.mkdir();
			parameterHandler.make(command);
			
//			输入一个 output 文件,执行输出增加 -E 选项
			GetDetectedTasks getDetectTasks = new GetDetectedTasks(makeFolder,outFolder);
			getDetectTasks.deal();
			tasks = getDetectTasks.getTaskList();
			
//			增加处理流程, 如果标准输出抓取不到任何tasks, 尝试读取关闭silent选项(打开verbose选项)的all文件进行抓取
			if(tasks.isEmpty()){
//				针对all文件进行抓取
				parameterHandler.captureFromAll();
//				重新尝试获取tasks
				getDetectTasks.deal();
				tasks = getDetectTasks.getTaskList();
			}
//			将tasks存储为json文件
			storeTasksToJson(outFolder + "/tasks.json");
			return true;
		}
		return false;
	}
	
	/*
	 * 将tasks存储为json文件
	 * @param jsonFilePath: json文件存储的地址
	 */
	private void storeTasksToJson(String jsonFilePath){
	    //    写文件
	    Type myType = new TypeToken<LinkedList<LinkedList<String>>>(){}.getType();
		try {
			JsonWriter writer = new JsonWriter(new FileWriter(jsonFilePath));
			Gson gson = new GsonBuilder().create();
			gson.toJson(tasks, myType, writer);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException("output transform to json failed");
		}
	}

	/*
	 * 从json文件中恢复出tasks
	 *  @param jsonFilePath: json文件存储的地址
	 */
	public static List<List<String>> getTasksFromJson(String jsonFilePath){
	    Type myType = new TypeToken<LinkedList<LinkedList<String>>>(){}.getType();
		try {
			JsonReader reader = new JsonReader(new FileReader(jsonFilePath));
			tasks = new Gson().fromJson(reader, myType);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("json to tasks failed");
		}
		return tasks;
	}
	
	/**
	 * 
	 * @return task1: List<fileName>, task2: List<fileName> ...
	 */
	public List<List<String>> getTasks(){
//		得到生成的所有的task路径, 如果当前的工程没有生成可执行文件,就返回一个空列表
		return tasks;
	}
	
	public void clean(){
//		执行make clean 然后删除 ProcessLog
		if(_projectDirectory != null && _outputDirectory != null){
//			必须执行过run() 之后才能clean
			Execute.executeCommand("cd " + _projectDirectory + " && make clean");
			Execute.executeCommand("cd "+ _outputDirectory + " && rm -r .process_makefile");
		}
	}
	
	public  boolean makeWithPath(String path, String makeCommand){
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
		return make(makeCommand);
	}

	public  void makeClean(){
//		make clean, 清除所有的 .o 文件, 但是需要先执行 run() 才能调用
		if(_projectDirectory != null)
			Execute.executeCommand("cd " + _projectDirectory + " && make clean");
	}

	public boolean makeWithPathCleanO(String path, String makeCommand){
//		带有路径地 run 完 clean 所有的 .o 文件
		boolean result = makeWithPath(path, makeCommand);
		if(result){
			makeClean();
		}
		return result;
	}
	
	public boolean makeCleanPointO(String makeCommand){
//		run 完之后 clean 所有的 .o 文件
		boolean result = make(makeCommand);
		if(result){
			makeClean();
		}
		return result;
	}
	
	
}
