package cn.harry.mapper;

import java.util.Stack;


import com.google.common.collect.RangeMap;
import com.mapping.SourceOriginMapping;
import com.mapping.TextCorresponding;
import com.not.so.common.Pair;

public class SourceLocationMapper {
	/*
	 * 提供特定的接口以调用 location mapper
	 */
	
	private String _filename = null;
	private TextCorresponding _textCorresponding = null;
	private SourceOriginMapping _mapping = null;
	
	public SourceLocationMapper(String filename){
		_filename = filename;
	}
	
	public boolean load(){
//		读取一个.i文件,并分析其来源
		if(_filename == null){
			return false;
		}
//		先处理一下用户目录的问题 ~无法识别
		if(_filename.charAt(0) == '~'){
			_filename = System.getProperty("user.home") + _filename.substring(1);
		}
		_textCorresponding = new TextCorresponding(_filename);
		_mapping = new SourceOriginMapping();
//		load
		if(!_textCorresponding.dealByLine(_mapping)){
			return false;
		}
		return true;
	}

	public Stack<Pair> resolve(int lineNumber){
//		得到所需某行 .i 文件的源文件列表 没找到就返回一个 null
		Stack<Pair> result = _mapping.get(_filename, lineNumber);
		if(result == null){
			return null;
		}
		return result;
	}
	
	public RangeMap<Integer, Stack<Pair>> getRange(){
		return _mapping.getRange(_filename);
	}
	
	public static void main(String[] args){
		SourceLocationMapper sourceLocationMapper = new SourceLocationMapper("/home/harry/Desktop/grep.o.i");
//		测试
		sourceLocationMapper.load();
//		System.out.println(getRange());
//		System.out.println(sourceLocationMapper.resolve(946));
		System.out.println(sourceLocationMapper.resolve(10478));
//		System.out.println(sourceLocationMapper.getRange());
	}
}
