package com.dw;

import com.not.so.common.Pair;
import com.util.Execute;

import cn.harry.captor.Task;
import cn.harry.captor.Tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public class GetDetectedTasks {
	/*
	 * 处理一个入文件,文件里面是待处理的GCC指令
	 * 处理后按照可执行文件task分类,每一个类由一堆.i文件组成,代表一个测试集
	 */
	String outFolder = null, makeFolder = null;
	FileInputStream fis = null;
	FileOutputStream fos = null;
	BufferedWriter out = null;
	BufferedReader in = null;
	int taskNumber = 0;
	int lineNumber = 0;
	Stack<String> dirStack = new Stack<String>();
//	修改正则式..前后忘了加空格
	Pattern pattern_cS = Pattern.compile(" -[cS] ");
	Pattern pattern_nonExecutive = Pattern.compile(" -[cSE] ");
//	开头不一定是gcc 也可能是带选项的gcc
	Pattern pattern_startWithCC = Pattern.compile("^\\s*([/a-z0-9-_]*-)?g?cc.*");
	Pattern pattern_out = Pattern.compile("-o\\s+([^\\s]+)");
	Pattern pattern_filename = Pattern.compile(" (([^ ]+)\\.[c|cc|C|cxx|cpp])");
	Pattern pattern_sorcefileSuffix = Pattern.compile(".[c|cc|C|cxx|cpp]$");
	Pattern pattern_startWithAr = Pattern.compile("^\\s*([/a-z0-9-_]*-)?ar ");
	Pattern pattern_sharedlib = Pattern.compile(" -shared ");
	Pattern intoFolder = Pattern.compile("Entering directory ['\"`]([^']+)['\"`]");
	Pattern outofFolder = Pattern.compile("Leaving directory ['\"`]([^']+)['\"`]");
//	当前文件路径 由输入文件指定
	String abslutePath = System.getProperty("user.dir") + '/';
	
	HashMap<String, Pair> fileMap = new HashMap<>();
	HashMap<String, List<String>> libMap = new HashMap<>();
	
//	每次生成或者移动 .i 文件到某个 task 文件夹的时候,都存一下
	Tasks tasks = new Tasks();
//	抓取的时候可以指定宏
	String macros = null;
	
	public GetDetectedTasks(String makeFolder,String outFolder){
		this.makeFolder = makeFolder;
//		查看输出是不是绝对路径,不是的话进行转换
		if(outFolder.charAt(0) != '/'){
			this.outFolder = abslutePath + outFolder;
		} else{
			this.outFolder = outFolder;
		} 
//		更改当前工程路径
		if(makeFolder.charAt(0) == '/'){
			abslutePath = makeFolder + '/';
		}
		else{
			abslutePath += makeFolder + '/';
		}
		dirStack.push(abslutePath);
	}
	
	public BufferedReader getReader(String filename){
		File inFile = new File(filename);
		BufferedReader result = null;
		try {
			fis = new FileInputStream(inFile);
			InputStreamReader isr = new InputStreamReader(fis);
			result = new BufferedReader(isr);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return result;
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
	
	public int getTaskNumber(){
		return this.taskNumber;
	}
	
	public Tasks getTaskList(){
	    return this.tasks;
	}
	
	public void deal() {
//		处理输入文件,得到输出文件,输出文件都存放到 ProcessLog 文件夹里面
		in = getReader(outFolder + "/output");
		out = getWriter(outFolder + "/outputParsing");
		String line = null;		
		try {
			while((line = in.readLine()) != null){
				out.write(parse(line));
				out.newLine();
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				in.close();
				out.close();
				fis.close();
				fos.close();
			} catch(IOException e){
				e.printStackTrace();
			}
		}
		
//		处理结束,将得到的Map 都分别存储到文件里面
		out = getWriter(outFolder + "/libMap");
		try {
			out.write(libMap.toString());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				out.close();
			} catch(IOException e){
				e.printStackTrace();
			}
		}
		
		out = getWriter(outFolder + "/fileMap");
		try {
			out.write(fileMap.toString());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				out.close();
			} catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	public void executeCommand(String command){
		System.out.println(command+"/n");
//		执行一条linux命令 如果命令执行不成功 就直接产生一个异常
		if(!dirStack.empty()){
			command = "cd " + dirStack.lastElement() + " && " + command;
		}
		String[] commands = {command};
		Execute.executeCommands(commands);
	}
	
	public String toAbsolutePath(String folderString,String relativePath){
		if(relativePath.startsWith("/")) {
			return relativePath;
		}
		File folder = new File(folderString);
		File tempRelativeFile = new File(folder,relativePath);
		try {
			relativePath = tempRelativeFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return relativePath;
	}
	
	public boolean hasInput(String option){
		option = option.trim();
//		判断该命令后面是否有输入文件
		if(option.equals("-aux-info") 
				|| option.equals("-include") || option.equals("-imacros") || option.equals("-iprefix")
				|| option.equals("-MF") || option.equals("-MT") || option.equals("-MQ"))
			return true;
		return false;
	}
	
	public List<String> getInputFileListNumbers(String line, String currentFolder, 
			List<Integer> inputfileNumberInParts){
//		考虑多个空格分割的情况 用正则式 \\s+
//		得到 mutable 的array 必须得将 asList 得到的 list 再转化成 ArrayList
		List<String> partStrings = new ArrayList<String>(Arrays.asList(line.split("\\s+")));
		for(int i = 1;i<partStrings.size();++i){
			String partString = partStrings.get(i);
			if(partString.charAt(0) == '-'){
				if(hasInput(partString))
					++i;
				continue;
			}
			partString = toAbsolutePath(currentFolder, partString);
			partStrings.set(i, partString);
			inputfileNumberInParts.add(i);
		}
		return partStrings;
	}
	
	public String getTasks(String line,String currentFolder){
//		输入是一个会得到可执行文件的指令,需要更改这个指令,并把所有需要的 .i 文件放到一个文件夹里面
		taskNumber++;
		if(taskNumber == 6) {
			System.out.println("");
		}
		String result = "";
		String taskName = "";
		String folder = outFolder + "/task" + String.valueOf(taskNumber);
		File dirFile = new File(folder);
		dirFile.mkdir();
		
//		记录一下task文件夹里面的 .i 文件
		List<String> taskiFiles = new LinkedList<String>();
		
//		去掉输出文件并记录生成的可执行文件名称
		Matcher matcher = pattern_out.matcher(line);
		if(matcher.find()){
			taskName = matcher.group(1);
			result = currentFolder + taskName;
			line = matcher.replaceAll("");
		}
		
//		得到输入文件列表, inputfileNumberInParts记录了输入文件在partString中的编号
		List<Integer> inputfileNumberInParts = new ArrayList<>();
		List<String> partStrings = getInputFileListNumbers(line, currentFolder, inputfileNumberInParts);
		
//		新增一个Set来判断某个文件是否已经被加到数据结构里面, 
//		比如gcc ../lib/libgreputils.a ../lib/libgreputils.a 调用两次lib文件, 只需要处理一次, 否则会产生重复
		Set<String> processedFile = new HashSet<>();
		
//		检测 ar 然后检测其他输入, 并替换, 替换 .o 文件
		String tempString = "";
		int index = 0;
		for(int i = 0;i<inputfileNumberInParts.size();++i){
			index = inputfileNumberInParts.get(i);
			tempString = partStrings.get(index);
//			tempString之前已经处理过, 不再重复处理
			if(processedFile.contains(tempString)) {
				partStrings.set(index, null);
				continue;
			} else {
				processedFile.add(tempString);
			}

			if(libMap.containsKey(tempString)){
				partStrings.set(index, null);
				List<String> ofileStrings = libMap.get(tempString);
				for(int j = 0; j < ofileStrings.size(); ++j){
					inputfileNumberInParts.add(partStrings.size());
					partStrings.add(ofileStrings.get(j));
				}				
			} else if(fileMap.containsKey(tempString)) {
				partStrings.set(index, fileMap.get(tempString).getFirst());
			}
		}
		matcher = pattern_filename.matcher(line);
		
		String nonSourcefileParts = "";
		int iter = 0;
//		去掉刚才读到的输入文件
		for(int i = 0;i<partStrings.size();++i){
//			注意检查iter 的范围
			if(iter >= inputfileNumberInParts.size() || i != inputfileNumberInParts.get(iter)){
				nonSourcefileParts += (" " + partStrings.get(i));
			}else {
				++iter;
			}
		}
		
//		有可能有多个源文件输入
		while(matcher.find()){
//			输入文件包含源文件的情况 先将源文件编成 .i 文件,直接生成到目标文件夹里面
			line = nonSourcefileParts;
			
//			加上匹配到的源文件,注意空格分割
			String outFileName = toAbsolutePath(currentFolder, matcher.group(1));
			line += (" " + outFileName);
//			去除前缀和后缀
			String outFileNameWithoutSuffix = matcher.group(2);
			iter = outFileNameWithoutSuffix.length()-1;
			while(iter >= 0 && outFileNameWithoutSuffix.charAt(iter) != '/'){
				--iter;
			}
			outFileNameWithoutSuffix = outFileNameWithoutSuffix.substring(iter+1);
//			建立对应关系
			fileMap.put(outFileName, new Pair(toAbsolutePath(folder,outFileNameWithoutSuffix + ".i"),lineNumber));
//			直接生成到文件夹
			getPreprocessedFiles(line, currentFolder, folder + "/" + outFileNameWithoutSuffix + ".i");
//			记录一下
			taskiFiles.add(outFileNameWithoutSuffix+".i");
		} 
		
//		直接把所有关联的文件都移动到刚创建的文件夹里面
//		获取所有的 .i 文件
		List<String> commands = new ArrayList<>();
		for(int i = 0;i<inputfileNumberInParts.size();++i){
//			匹配结尾,结尾是源文件就不复制
			tempString = partStrings.get(inputfileNumberInParts.get(i));
			if(tempString == null)
				continue;
			matcher = pattern_sorcefileSuffix.matcher(tempString);
			if(!matcher.find()){
//				输入文件有不能找到匹配的文件(.o不能对应.i), task 生成失败
				if(!tempString.endsWith("i")) {
					if(tempString.startsWith("/usr/lib")) {
						continue;
					}
//					保留残余的task
					continue;
//					taskNumber--;
//					Execute.executeCommand("rm -r " + folder);
//					return result + " Failed. ";
				}
//				提取后缀
				iter = tempString.length()-1;
//				增加处理, 用行号加文件名当新的文件名, 避免重复, 如, line185-imap-util.o.i
				while(iter >= 0 && tempString.charAt(iter) != '/'){
					--iter;
				}
				--iter;
				while(iter >= 0 && tempString.charAt(iter) != '/'){
					--iter;
				}
				String taskFileName = tempString.substring(iter+1);
				taskFileName = taskFileName.replace('/', '-');
				taskiFiles.add(taskFileName);
				commands.add("cp " + tempString + " " + folder + "/" + taskFileName);
			}
		}
//		只有当输入文件里面有中间文件的时候才执行复制
		if(!commands.isEmpty()){
			boolean executeResult = true;
			executeResult = Execute.executeCommands((String[])commands.toArray(new String[commands.size()]));
			if(executeResult) {
				tasks.addTask(taskName, Task.of(folder, taskiFiles, taskName));	
			}
			else {
//				generate failed
				taskNumber--;
				Execute.executeCommand("rm -r " + folder);
			}
		}
		else {
			tasks.addTask(taskName, Task.of(folder, taskiFiles, taskName));		
		}
		return result;
	}
	
	public String getPreprocessedFiles(String line, String currentFolder, String outFileName) {
//		输入是一个会得到中间输出文件的指令，需要修改这个指令，把所有的输出都变成 .i 的形式
//		直接输出到 .o 文件所在的相同文件夹里,便于后面替换
//		第二个参数可以指定 输出目标地址 必须是一个绝对路径
		
		String folder = outFolder + "/line" + String.valueOf(lineNumber);
		File dirFile = new File(folder);
		dirFile.mkdir();
		String command = "";
//		增加一个 -g 选项, 输出的时候输出一下当前编译的绝对路径
		String result = line + " -E -g -O0";
		if(outFileName.equals("")){		
			Matcher matcher = pattern_out.matcher(result);
			String changedFilename = "";
			if(matcher.find()){
				outFileName = matcher.group(1);
//				提取前缀
				int iter = outFileName.length()-1;
				while(iter >= 0 && outFileName.charAt(iter) != '/'){
					--iter;
				}
				changedFilename = toAbsolutePath(folder, outFileName.substring(iter+1) + ".i");
				result = matcher.replaceAll("-o " + changedFilename + " ");
			} else{
//				没指定输出名称,这时 gcc 会默认输入名称加 .o 或者 .s 为输出名称
				matcher = pattern_filename.matcher(result);
				if(matcher.find()){
					outFileName = matcher.group(2);
				}
				if(outFileName.indexOf(" -S ") >= 0){
					outFileName += ".s";
				} else{
					outFileName += ".o";
				}
				changedFilename = toAbsolutePath(folder, outFileName + ".i");
				result += (" -o " + changedFilename);
			}
//			建立对应关系
			outFileName = toAbsolutePath(currentFolder, outFileName);
			fileMap.put(outFileName, new Pair(changedFilename,lineNumber));
				
		} else {
			result += (" -o " + outFileName); 
		}
		command += result;
		executeCommand(command);

		return result;
	}
	
	public void getLibMap(String line,String currentFolder){
//		处理 ar 指令,提取出输入文件 .o 和输出文件 .a 存绝对路径
//		bugFix: should split with multiple spaces
		String[] partStrings = line.split("\\s+");
		if(partStrings.length < 4){
			return ;
		}
		String outputFilename = toAbsolutePath(currentFolder, partStrings[2]);
		List<String> inputFilenames = new ArrayList<>();
		inputFilenames.add(toAbsolutePath(currentFolder, partStrings[3]));
		int iter = 4;
		while(iter < partStrings.length){
			inputFilenames.add(toAbsolutePath(currentFolder, partStrings[iter]));
			++iter;
		}
		libMap.put(outputFilename, inputFilenames);
	}
	
	public void getSharedMap(String line, String currentFolder){
//		去掉输出文件
		String outputFilename = "";
		Matcher matcher = pattern_out.matcher(line);
		if(matcher.find()){
			outputFilename = toAbsolutePath(currentFolder, matcher.group(1));
			line = matcher.replaceAll("");
		}
//		得到输入文件列表,记录编号
		List<Integer> inputfileNumberInParts = new ArrayList<>();
		List<String> partStrings = getInputFileListNumbers(line, currentFolder, inputfileNumberInParts);
		List<String> storeInput = new ArrayList<>();
		for(int i = 0;i<inputfileNumberInParts.size();++i){
			storeInput.add(partStrings.get(inputfileNumberInParts.get(i)));
		}
		libMap.put(outputFilename, storeInput);
	}
	
	public String parse(String line){
//		处理一行的详细步骤
		lineNumber++;
		String result = "";
//		判断是否指定了宏
		if(macros != null)
			line = line + macros;
//		处理之前应该先把双引号换成空格, 防止文件名出错
//		修改了正则表达式而不是修改抓取结果, 防止将gcc语句中的"替换掉
//		line = line.replace('\"', ' ');
//		line = line.replace('`', '\'');
//		处理文件夹的切换,每遇到进入一个文件夹就进栈,离开一个文件夹就弹栈
		Matcher matcher = intoFolder.matcher(line);
		if(matcher.find()){
			dirStack.push(matcher.group(1)+'/');
			return result;
		}
		matcher = outofFolder.matcher(line);
		if(matcher.find() && !dirStack.empty()){
			dirStack.pop();
			return result;
		}
		
//		得到当前执行路径
		String currentFolder = abslutePath;
		if(!dirStack.empty())
			currentFolder = dirStack.lastElement();
		
//		开头得是 cc 或者 gcc
		matcher = pattern_startWithCC.matcher(line);
		if(matcher.matches()){
			if(line.endsWith("'")){
				return result;
			}
//			处理 -c -S 的情况
			matcher = pattern_cS.matcher(line);
			if(matcher.find()){
				return getPreprocessedFiles(line,currentFolder,"");
			}		
//			处理 -shared 命令的情况
			matcher = pattern_sharedlib.matcher(line);
			if(matcher.find()){
				getSharedMap(line, currentFolder);
				return result;
			}
//			处理得到可执行文件的情况
			matcher = pattern_nonExecutive.matcher(line);
			if(!matcher.find()){
				return getTasks(line,currentFolder);
			}
		}
		
//		处理 ar 命令的情况
		matcher = pattern_startWithAr.matcher(line);
		if(matcher.find()){
			getLibMap(line, currentFolder);
			return result;
		}
		
//		其他情况不做处理,返回空行
		return result;
	}

	public void setMacros(Map<String, String> macroMap) {
		macros = "";
		for(String macro : macroMap.keySet()) {
			String value = macroMap.get(macro);
			if(value == null){
				macros = macros + " -D" + macro;
			}
			else {
				macros = macros + " -D\"" + macro +"=" + value + "\"";
			}
		}
	}
	
	public void setMacros(List<String> macroList) {
		macros = "";
		for(String macro : macroList) {
			if(macro.contains("=")) {
				macros = macros + " -D\"" + macro + "\"";
			}
			else {
				macros = macros + " -D" + macro;
			}
		}
	}
	
	public void setMacros(String macros) {
		this.macros = macros;
	}

	public static void main(String[] args) {
//		Pattern pattern_startWithCC = Pattern.compile("^\\s*([/a-z0-9-_]*-)?g?cc.*");
//		String line = "gcc-7 -c -DSTDC_HEADERS=1 -DHAVE_UNISTD_H=1 -DDIRENT=1 -O gzip.c";
//		Matcher matcher = pattern_startWithCC.matcher(line);
//		System.out.println(matcher.matches());
		String line = "ar  r ../libcrypto.a cryptlib.o mem.o mem_dbg.o cversion.o ex_data.o cpt_err.o ebcdic.o uid.o o_time.o o_str.o o_dir.o mem_clr.o";
		String[] partStrings = line.split("\\s+");
		for (String a : partStrings) {
			System.out.println(a);
		}
	}

}
