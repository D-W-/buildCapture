package com.not.so.common;

public class Pair implements Cloneable{
    /**
	 * 一个简单的 Pair 类型,在此工程中 第一个参数 是文件绝对地址 第二个参数是行号
	 */
	private String first;
    private Integer second;

    public Pair(String first, Integer second) {
    	super();
    	this.first = first;
    	this.second = second;
    }

    public int hashCode() {
    	int hashFirst = first != null ? first.hashCode() : 0;
    	int hashSecond = second != null ? second.hashCode() : 0;

    	return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
    	if (other instanceof Pair) {
    		Pair otherPair = (Pair) other;
    		return 
    		((  this.first == otherPair.first ||
    			( this.first != null && otherPair.first != null &&
    			  this.first.equals(otherPair.first))) &&
    		 (	this.second == otherPair.second ||
    			( this.second != null && otherPair.second != null &&
    			  this.second.equals(otherPair.second))) );
    	}

    	return false;
    }

    public String toString()
    { 
           return "( ABSOLUTE_FILE_PATH: " + first + " ,  LINE_NUMBER: " + String.valueOf(second) + ")\n";
    }

    public String getFirst() {
    	return first;
    }

    public void setFirst(String first) {
    	this.first = first;
    }

    public Integer getSecond() {
    	return second;
    }

    public void setSecond(Integer second) {
    	this.second = second;
    }
    
    public void addSecond(int num){
    	this.second += num;
    }
    
    @Override
    public Object clone(){
//    	为了后面的需要复制当前的栈,就麻烦一下,但是还是需要深度clone,还是挺麻烦的..
    	Pair pair = null;
    	try{
    		pair = (Pair)super.clone();
    	} catch(CloneNotSupportedException e){
    		e.printStackTrace();
    	}
    	return pair;
    }
    
    public Pair(Pair another){
//    	差点忘了重写一个拷贝构造函数..
    	this.first = another.first;
    	this.second = another.second;
    }
}