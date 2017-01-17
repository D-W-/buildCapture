package cn.harry.captor;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.util.Execute;


public class Task {
	/*
	 * （目标名字、源文件数目、源文件列表、源代码处理前后的代码行数（可以映射的具体文件）
	 */
	private List<String> files = new LinkedList<>();
	private List<Integer> linesAfterProcess = new LinkedList<>();

	private int size;
	private String rootDir;
	private String taskName;
	private int allCodeLinesAfterProcess;
	
	public Task(String rootDir, List<String> files, String taskName){
		this.rootDir = rootDir;
		this.size = files.size();
		this.allCodeLinesAfterProcess = 0;
		this.taskName = taskName;
		for(int i = 0; i < files.size(); ++i) {
			String file = rootDir + File.separator + files.get(i);
			this.files.add(file);
			int lineNumber = fileLineNumber(file);
			allCodeLinesAfterProcess += lineNumber;
			linesAfterProcess.add(lineNumber);
		}
	}
	
	public Task(List<String> files) {
		for(int i = 0; i < files.size(); ++i) {
			String file = files.get(i);
			this.files.add(file);
			int lineNumber = fileLineNumber(file);
			allCodeLinesAfterProcess += lineNumber;
			linesAfterProcess.add(lineNumber);
		}
	}
	
	private int fileLineNumber(String file) {
		if(!file.endsWith("i")) {
			return -1;
		}
		String query = "wc -l " + file + " | awk '{print $1}'";
		String codeLines = Execute.executeCommandandGetoutput(query).trim();
		return Integer.valueOf(codeLines);
	}
	
	public static Task of(String rootDir, List<String> files, String taskName){
		return new Task(rootDir, files, taskName);
	}
	
	public static Task of(List<String> files) {
		return new Task(files);
	}
	
	@Override
	public String toString(){
		return this.taskName + " : " + String.valueOf(this.allCodeLinesAfterProcess) + " lines.";
	}
	
	public Integer getSize() {
		return size;
	}
	public void setSize(Integer size) {
		this.size = size;
	}
	public List<String> getFiles() {
		return files;
	}
	public void setFiles(List<String> files) {
		this.files = files;
	}
	public List<Integer> getLinesAfterProcess() {
		return linesAfterProcess;
	}
	public void setLinesAfterProcess(List<Integer> linesAfterProcess) {
		this.linesAfterProcess = linesAfterProcess;
	}
	public String getRootDir() {
		return rootDir;
	}
	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public int getAllLines() {
		return this.allCodeLinesAfterProcess;
	}
}
