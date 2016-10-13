package com.mapping;

import com.not.so.common.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

public class SourceOriginMapping {
	/*
	 * 这是用于实现 .i 文件和源文件(c文件)对应的代码
	 * 存储结构是这样的
	 * hash_map< filename , range_map< line_num, pair< stack<String> , delta > > >
	 * filename 是 .i 文件的绝对地址
	 * line_num 是 .i 文件中的行号
	 * stack<String> 是 .i 文件中的文件引用链(栈是倒序的)
	 * delta 是源文件中代码块的起始行号 (减去) .i 文件的起始行号
	 */
	
	private final Map<String, RangeMap<Integer, Stack<Pair>>> mapping = new HashMap<>();
	
	public void set(String filename,Stack<Pair> chain,int startLine,int currentLine){
		RangeMap<Integer, Stack<Pair>> fileMapping = mapping.get(filename);
	    if (fileMapping == null) {
	        fileMapping = TreeRangeMap.create();
	        mapping.put(filename, fileMapping);
	    }
	    Range<Integer> range = Range.closedOpen(startLine, currentLine);
//	    由于这里需要深复制,所以得遍历一下
	    Stack<Pair> storePairs = new Stack<Pair>();
	    for(int i = 0;i < chain.size();++i){
	    	storePairs.add((Pair)chain.get(i).clone());
	    }
//	    源文件起始行号 减去 .i 文件的起始行号 (仅限于栈顶)
	    storePairs.lastElement().setSecond(storePairs.lastElement().getSecond() - startLine - (currentLine - startLine));
	    fileMapping.put(range, storePairs);
	}
	
	public Stack<Pair> get(String filename, int lineNumber){
		RangeMap<Integer, Stack<Pair>> fileMapping = mapping.get(filename);
		if(fileMapping != null){
			Stack<Pair> storePairs_backup = fileMapping.get(lineNumber);
//		    由于这里需要深复制,所以得遍历一下
		    Stack<Pair> storePairs = new Stack<Pair>();
		    for(int i = 0;i < storePairs_backup.size();++i){
		    	storePairs.add((Pair)storePairs_backup.get(i).clone());
		    }
		    
			if(storePairs != null){ 
//				.i 文件中的行号 加上 存储的行号 (仅限于栈顶)
				storePairs.lastElement().setSecond(storePairs.lastElement().getSecond() + lineNumber);
				return storePairs;
			}
		}
		return null;
	}
	
	public RangeMap<Integer, Stack<Pair>> getRange(String filename){
		return mapping.get(filename);
	}

}
