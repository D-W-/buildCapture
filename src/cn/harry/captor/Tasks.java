package cn.harry.captor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Tasks {
	private LinkedList<Task> tasks = new LinkedList<>();
	private Map<String, Integer> nameToTaskNumber = new HashMap<>(); 
	private int number;
	
	public static Tasks fromTask(Task task) {
		Tasks tasks = new Tasks();
		tasks.addTask(task.getTaskName(), task);
		return tasks;
	}
	
	public Tasks() {
		number = 0;
	}

	public Task getTask(int taskNumber) {
		return tasks.get(taskNumber);
	}
	
	public Task getTask(String taskName) {
		Integer taskNumber = nameToTaskNumber.get(taskName);
		if(taskNumber == null)
			return null;
		return tasks.get(taskNumber);
	}
	
	public void addTask(String taskName, Task task){
		tasks.add(task);
		nameToTaskNumber.put(taskName, tasks.size()-1);
		number = tasks.size();
	}
	
	@Override
	public String toString() {
		String result = "";
		for(Task task : tasks) {
			result += task.toString();
			result += "\n";
		}
		return result;
	}

	public LinkedList<Task> getTasks() {
		return tasks;
	}

	public int size() {
		return number;
	}
}
