package com.mapping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Stack;

import com.google.common.primitives.UnsignedBytes;
import com.mapping.SourceOriginMapping;
import com.not.so.common.Pair;

public class TextCorresponding {
	/*
	 * 读入某个文件,该文件是以 .i 为结尾的预编译文件,根据注释建立该文件和源文件的行号对应
	 * 执行方式就是在jar包内传入某个文件 输出是传入文件名加上 .out 
	 */
	
//	.i 文件来源文件的地址
	String sourceFilename;
//	当前 .i 文件的被编译环境的绝对地址
	String absolutePath;
//	输入文件需要是绝对地址
	String filename;
//	栈,用于存储链式引用记录
	Stack<Pair> includeStack = new Stack<Pair>();
//	行号指针,标识当前读到了第几行
	int linePointer = 0;
//	标识,上一行是不是代码
	boolean isLastLineCode = false;
//	.i 文件代码块的起始行号
	int startLine = 0;
//	用于写文件的流
	FileOutputStream fos = null;
	
	public TextCorresponding(String filename) {
		this.filename = filename;
	}
	
	public boolean dealByLine(SourceOriginMapping mapping){
//		文件不存在就返回flase
		File inFile = new File(filename);
		if(!inFile.exists()){
			return false;
		}
		String line = null;
		BufferedReader bReader = null;
		try {
			bReader = 
					new BufferedReader(
						new InputStreamReader(
								new FileInputStream(inFile)));
//			.i 文件有严格的行数定义,如果第一行是空行,或者不符合定义格式的任何行都不属于 .i 文件 返回false
//			先对第一行处理,得到文件名
			if((line = bReader.readLine()) != null){
				if(!firseLine(line))
					return false;
			}
//			再对第二行处理,得到编译环境的绝对路径
			if((line = bReader.readLine()) != null){
				if(!secondLine(line))
					return false;
			}
			linePointer = 2;
			while((line = bReader.readLine()) != null){
				linePointer++;
				record(line, mapping);
			}
			
//			最后一个代码块处理
			if(linePointer > 1 && !includeStack.empty()){
				mapping.set(this.filename, includeStack, startLine, linePointer + 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				bReader.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean firseLine(String line){
//		第一行里面可以提取出来源文件的名称 先在stack里面添加当前文件
		String[] parts = line.split(" ");
		if(parts.length < 3 || !parts[0].equals("#") || !parts[1].equals("1"))
			return false;
		this.sourceFilename = parts[2].substring(1, parts[2].length()-1);
		return true;
	}
	
	public boolean secondLine(String line){
//		经过更改,增加了 -g 选项, 第二行里面可以提取出来 .i 文件被编译的环境的绝对路径
		String[] parts = line.split(" ");
		if(parts.length < 3 || !parts[0].equals("#") || !parts[1].equals("1"))
			return false;
		this.absolutePath  = parts[2].substring(1, parts[2].length()-2);
//		每次入栈都要检查是不是绝对地址
		if(this.sourceFilename.charAt(0) != '/'){
			this.sourceFilename = this.absolutePath + this.sourceFilename;
		}
		includeStack.push(new Pair(this.sourceFilename, 1));
		return true;
	}
	
	public void record(String line,SourceOriginMapping mapping){
		if(!line.equals("") && line.charAt(0) == '#'){
			if(isLastLineCode){
//				如果上一行是code,这一行变成了注释,就需要存一下
//				使用 SourceOriginMapping 
				mapping.set(this.filename, includeStack, startLine, linePointer);
			}
			isLastLineCode = false;
			
			String[] parts = line.split(" ");
			if(parts.length <= 3){
//				这一行后面没有flag // 去掉 或者 这一行是进入某个文件的flag
				if(parts.length == 3){
//					行号校对
					includeStack.lastElement().setSecond(Integer.valueOf(parts[1]));					
				}
				return ;
			}
			int lineNumber = 0;
			try {
				lineNumber = Integer.valueOf(parts[1]);
			} catch (NumberFormatException e) {
				return;
			}
			String filePath = parts[2].substring(1, parts[2].length()-1);
//			转换绝对路径
			if(filePath.charAt(0) != '/'){
				filePath = this.absolutePath + filePath;
			}
			if(parts[3].equals("2")){
//				弹栈,之后栈顶行号加1
				includeStack.pop();
				if(!includeStack.empty()){
					includeStack.lastElement().addSecond(1);
				}
			} 
			
			if(parts[2].charAt(1) == '<'){
//				这一行不是一个line绝对地址
				return ;
			}
			
			if(!includeStack.empty() && filePath.equals(includeStack.lastElement().getFirst())){
//				与栈顶元素相同,直接更改栈顶的行号
				includeStack.lastElement().setSecond(lineNumber);
			} else {
//				正常调用,直接入栈
				includeStack.push(new Pair(filePath, lineNumber));
			}
			
		} else{
			if(!isLastLineCode){
//				上一行是注释,这一行是代码,就标记这个代码块的开始
				startLine = linePointer;
			}			
			if(!includeStack.empty()){
				includeStack.lastElement().addSecond(1);
			}

			if(line.equals(""))
				return ;
//			空行不改变状态
			isLastLineCode = true;
			
		}
	}
	
	public BufferedWriter getWriter(String filename) {
		File outFile = new File(filename);
		BufferedWriter result = null;
		try {
			fos = new FileOutputStream(outFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos);
			result = new BufferedWriter(osw);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void mainTest(String[] args) {
//		测试
//		TextCorresponding textCorresponding = new TextCorresponding("out.i");
//		SourceOriginMapping mapping = new SourceOriginMapping();
//		textCorresponding.dealByLine(mapping);
//		
//		System.out.println(mapping.getRange("out.i"));
		
		String filename = "";
		for(int i = 0;i<args.length;++i){
			filename = args[i];
			TextCorresponding textCorresponding = new TextCorresponding(filename);
			SourceOriginMapping mapping = new SourceOriginMapping();
			textCorresponding.dealByLine(mapping);

			BufferedWriter bufferedWriter = textCorresponding.getWriter(filename+".out");
			try {
				bufferedWriter.write(mapping.getRange(filename).toString());
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				try{
					bufferedWriter.close();
					textCorresponding.fos.close();
				} catch(IOException e){
					e.printStackTrace();
				}
			}

		}
		
	}

}
