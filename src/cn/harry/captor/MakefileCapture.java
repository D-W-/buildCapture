package cn.harry.captor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

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
	
	public static final String SHELL = "sh";
	public static Tasks tasks;
	
	String makeFolder = null;
	String outFolder = null;
	
	public MakefileCapture(String projectDirectory, String outputDirectory){
		_projectDirectory = projectDirectory;
		_outputDirectory = outputDirectory;
	}
	
	public MakefileCapture(String projectDirectory){
		_projectDirectory = projectDirectory;
		_outputDirectory = projectDirectory;
	}
	
	public boolean make(){
		return make(null);
	}
	
	public boolean make(String makeCommand) {
		return make(makeCommand, SHELL);
	}
	
	public boolean make(String makeCommand, String shell, String macors) {
		if(buildAndCapture(makeCommand, shell)){
			tasks = getDetectedTasks(macors);
			storeAndPrint();
			return true;
		}
		return false;
	}
	
	public boolean make(String makeCommand, String shell, Map<String, String> macroMap) {
		if(buildAndCapture(makeCommand, shell)){
			tasks = getDetectedTasks(macroMap);
			storeAndPrint();
			return true;
		}
		return false;
	}
	
	public boolean make(String makeCommand, String shell){
		if(buildAndCapture(makeCommand, shell)){
			tasks = getDetectedTasks();
			storeAndPrint();
			return true;
		}
		return false;
	}
	
	public boolean buildAndCapture(String makeCommand, String shell) {
//	    编译并抓取
		if(makeCommand == null || makeCommand.equals("")) {
			makeCommand = "make";
		}
		if(shell == null || shell.equals("")) {
			shell = SHELL;
		}
		if(_projectDirectory != null && !_projectDirectory.equals("")){
			makeFolder = _projectDirectory;
			outFolder = _outputDirectory;	
//			先处理一下用户目录的问题 ~无法识别
			if(makeFolder.charAt(0) == '~'){
				makeFolder = System.getProperty("user.home") + makeFolder.substring(1);
			}
			if(outFolder.charAt(0) == '~'){
				outFolder = System.getProperty("user.home") + outFolder.substring(1);
			}
			
//			对工程执行make，并抓取输出
			String command = "cd " + makeFolder + " && " + makeCommand;
			ParameterHandler parameterHandler = new ParameterHandler(outFolder);
			outFolder += "/.process_makefile";
			File rootFolder = new File(outFolder);
			try {
				outFolder = rootFolder.getCanonicalPath();
			} catch (IOException e) {
				throw new RuntimeException("Path Error" + e.toString());
			}
			rootFolder.mkdir();
			parameterHandler.make(command, shell);
			return true;
		}
		return false;
	}
	
	/*
	 * 输入一个 output 文件,执行输出增加 -E 选项
	 */
	public Tasks getDetectedTasks() {
		GetDetectedTasks getDetectTasks = new GetDetectedTasks(makeFolder,outFolder);
		getDetectTasks.deal();
		return getDetectTasks.getTaskList();
	}
	
	/*
	 * 输入一个 output 文件,执行输出增加 -E 选项
	 */
	public Tasks getDetectedTasks(String macros) {
		GetDetectedTasks getDetectTasks = new GetDetectedTasks(makeFolder,outFolder);
		getDetectTasks.setMacros(macros);
		getDetectTasks.deal();
		return getDetectTasks.getTaskList();
	}
	
	/*
	 * 输入一个 output 文件,执行输出增加 -E 选项
	 */
	public Tasks getDetectedTasks(Map<String, String> macroMap) {
		GetDetectedTasks getDetectTasks = new GetDetectedTasks(makeFolder,outFolder);
		getDetectTasks.setMacros(macroMap);
		getDetectTasks.deal();
		return getDetectTasks.getTaskList();
	}
	
	public void storeAndPrint() {
//		将tasks存储为json文件
		storeTasksToJson(outFolder + "/tasks.json");
//		统计并输出
		printTasks(tasks);
	}
	
	public static void printTasks(Tasks tasks) {
		System.out.println(tasks);
	}
	
	/*
	 * 将tasks存储为json文件
	 * @param jsonFilePath: json文件存储的地址
	 */
	private void storeTasksToJson(String jsonFilePath){
	    //写文件
	    Type myType = new TypeToken<Tasks>(){}.getType();
		try {
			JsonWriter writer = new JsonWriter(new FileWriter(jsonFilePath));
			Gson gson = new GsonBuilder().create();
			gson.toJson(tasks, myType, writer);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/*
	 * 从json文件中恢复出tasks
	 *  @param jsonFilePath: json文件存储的地址
	 */
	public static Tasks getTasksFromJson(String jsonFilePath){
	    Type myType = new TypeToken<Tasks>(){}.getType();
		try {
			JsonReader reader = new JsonReader(new FileReader(jsonFilePath));
			tasks = new Gson().fromJson(reader, myType);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		}
		return tasks;
	}
	
	/**
	 * 
	 * @return task1: List<fileName>, task2: List<fileName> ...
	 */
	public Tasks getTasks(){
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
