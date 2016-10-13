package com.test;

import java.util.Stack;

import com.google.common.collect.RangeMap;
import com.mapping.SourceOriginMapping;
import com.mapping.TextCorresponding;
import com.not.so.common.Pair;

public class TextCorresponder {
	
	public static String _filename = null;
	public static TextCorresponding _textCorresponding = null;
	public static SourceOriginMapping _mapping = null;
	
	public static boolean run(String filename){
		if(filename == null){
			return false;
		}
//		先处理一下用户目录的问题 ~无法识别
		if(filename.charAt(0) == '~'){
			filename = System.getProperty("user.home") + filename.substring(1);
		}
		_filename = filename;
		_textCorresponding = new TextCorresponding(filename);
		_mapping = new SourceOriginMapping();
		if(!_textCorresponding.dealByLine(_mapping)){
			return false;
		}
		
		return true;
	}
	
	public static Stack<Pair> getSourcePath(int lineNumber){
//		得到所需某行 .i 文件的源文件列表 没找到就返回一个 null
		Stack<Pair> result = _mapping.get(_filename, lineNumber);
		if(result == null){
			return null;
		}
		return result;
	}
	
	public static RangeMap<Integer, Stack<Pair>> getRange(){
		return _mapping.getRange(_filename);
	}

	public static void main(String[] args){
//		测试
		run("/home/harry/Desktop/process_makefile/test.i");
//		System.out.println(getRange());
		System.out.println(getSourcePath(946));
		System.out.println(getSourcePath(946));
		System.out.println(_mapping.getRange(_filename));
	}
}
