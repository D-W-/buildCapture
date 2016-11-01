package cn.harry.builder;

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
		String makefolder = "/home/harry/code/gzip-1.8";
		MakefileCapture makefileCapture = getCaptor(makefolder, makefolder);
		makefileCapture.make();
//		String outFolder = makefolder + "/.process_makefile";
//		GetDetectedTasks getDetectTasks = new GetDetectedTasks(makefolder,outFolder);
//		getDetectTasks.deal();
	}
}
