# ProcessMakefile

TODO: 等有时间根据现在的工程修改一下现在的文档
---
对由makefile控制的工程做一些处理, 以得到各个源文件预处理之后的文件,并保存对应关系

## 使用

### 1. 直接通过一个类来测试各个功能

在 com.test 下面有测试类 MakefileProcessor 来测试各个功能
这个类提供四个接口

1. `boolean run(String makeFolder, String outFolder)`: 传入工程根目录和想要输出的目录,执行工具,返回是否执行成功
2. `HashMap<String, List<String>> getTaskPaths()`: 得到工具生成的所有task 的绝对路径以及每个 task里面所有 .i 文件的列表
3. `void clean()`: 删除make产生的中间文件以及工具产生的 ProcessLog
4. `boolean runCleanO(String makeFolder, String outFolder)` 执行工具,并清除make产生的中间文件(.o)


### 2. 使用 jar 包来使用各个功能

将文件编译成 jar 包 
使用 `java -jar test.jar command parameters ...` 运行
后面的 `command parameters` 是输入参数

输入参数描述:

    java -jar test.jar INPUT_PATH make (-mko=OUTPUT_PATH)

 - INPUT_PATH: 需要 make 的工程的路径
 - make: 调用make
 - -mko=OUTPUT_PATH: 可选的, 可以指定输出 ProcessLog 的地址,如果不指定的话,默认生成到工程根目录下面, 也就是 INPUT_PATH + "/ProcessLog"

## 流程

1. 执行make指令,抓取make的输出,并保存
    - 进行了修改, 使用了语句`make \"SHELL=sh -xv\" -f makefile &>all >output` 进行输出
    - 抓取stdout和stderr的全部内容到all
    - 抓取stdout到output(另存一份stdout保证整个流程和以前差不多)
2. 利用上一步的输出文件逐条进行处理,并记录对应关系
    1. 对于 -c -o 这种对源文件的操作,生成相应文件夹下面的 `.i` 文件
    3. 对于生成的静态库和动态库都保存对应关系
    2. 对于没有 -c -o 这种生成可执行文件的操作,将输入对应的 `.i` 文件统一放入一个文件夹中
3. 最后将所有的对应关系写入文件

## 输出

1. line文件夹: 里面保存各行输出的 `.i` 文件
2. task文件夹: 里面保存输出可执行程序需要的所有 `.i` 文件
3. output: 保存make的输出结果(只保存stdout), 如果进行了打开静默开关的抓取, make的所有内容会保存在这里
4. outputParse: 保存转化后的各个命令
5. fileMap: 各种 .o .c 等文件的 **绝对路经** 与 .i 文件的 **绝对路径** 的对应关系
6. libMap: 各种动态静态链接库的 **绝对路径** 与 .o 文件的 **绝对路径**的对应关系
7. all: 保存make输出的所有内容(包括stdout和stderr)
8. output.old: 如果进行了打开静默开关的抓取, make的stdout会保存在这里

---

# TextCorresponderTool

这个工具是后面对于某个 .i 文件的某一行查询它对应的源文件具体在哪一行的工具

注意: 此工具处理的 .i 文件必须是加了 **-g** 参数生成的 .i 文件, 以此保证绝对路径的成功转换

## 使用

目前写了一个类 TextCorresponder 提供一些接口来调用这个工具

1. `boolean run(String filename)` 输入一个 .i 文件,处理并记录, 然后返回是否执行成功
2. `Stack<Pair> getSourcePath(int lineNumber)` 输入行号, 查询这行代码的源文件绝对路径和在源文件中的位置(一个Pair), 由于include是层层叠加的, 所以返回的是一个include的链
3. `RangeMap<Integer, Stack<Pair>> getRange()` 把经过`run()`处理得到的Range记录返回


