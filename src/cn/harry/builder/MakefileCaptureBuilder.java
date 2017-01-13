package cn.harry.builder;

import com.google.common.collect.ImmutableMap;

//import com.dw.GetDetectedTasks;
//import com.dw.GetDetectedTasks;

import cn.harry.captor.MakefileCapture;

public class MakefileCaptureBuilder {
	/*
	 * 建造某一特定类型的 captor
	 */

	public static MakefileCapture getCaptor(String projectDirectory, String outputDirectory){
		return new MakefileCapture(projectDirectory, outputDirectory);
	}
	
	public static void main(String[] args){
		ImmutableMap<String, String> macro = ImmutableMap.<String, String>builder().put("FIRST","").build();
		String makefolder = "/home/harry/learn/learnGCC";
		MakefileCapture makefileCapture = getCaptor(makefolder, makefolder);
		makefileCapture.make("make", "/bin/bash", macro);
//		makefileCapture.clean();
		
//		GetDetectedTasks getDetectTasks = new GetDetectedTasks(makefolder,makefolder + "/.process_makefile");
//		getDetectTasks.deal();
//		String outFolder = "/home/harry/code/dovecot/.debug";
//		GetDetectedTasks getDetectTasks = new GetDetectedTasks(makefolder,outFolder);
//		getDetectTasks.deal();
	}
}
