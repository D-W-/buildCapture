# ProcessMakefile

TODO: 等有时间根据现在的工程修改一下现在的文档
---
对由makefile控制的工程做一些处理, 以得到各个源文件预处理之后的文件,并保存对应关系

## 使用

### 1. 引入jar包使用接口调用

在 cn.harry 中提供了两个接口, 分别进行介绍

1. `cn.harry.MakefileCapture` 执行抓取, 处理抓取信息的全部流程
``` MakefileCapture captor = MakefileCaptureBuilder.getCaptor(projectDirectory, outputDirectory);
    makefileCapture.make("make", "/bin/bash");
    makefileCapture.clean();
```

第一句输入的`projectDirectory`是具有makefile的工程的根目录(可执行make的文件夹地址), `outputDirectory`是输出结果的指定目录, 默认生成到工程根目录下面的`.process_makefile`文件夹中

第二句输入的第一个参数是执行的make指令, 一般使用的都是`make`, 需要特殊参数可以加特殊参数, 第二个参数是指定执行make指令用的shell, 可以不指定, 有些项目使用`/bin/bash`, 有些使用`sh`, 这个暂时还没有固定的规律

第三句等同于执行`make clean && rm -r .process_makefile`, 如果需要执行第二遍编译抓取, 可以用这个接口或者执行前面的指令

2. `cn.harry.GetDetectedTasks` 只执行处理抓取信息的流程

对有makefile的文件夹抓取过之后, 可以只抓取一次, 然后每次调用这个接口就可以多次处理抓取信息

```
        GetDetectedTasks getDetectTasks = new GetDetectedTasks(makefolder,makefolder + "/.process_makefile");
        getDetectTasks.deal();
```

第一句输入的参数第一个是是具有makefile的工程的根目录(可执行make的文件夹地址), 第二个是抓取到的结果存放的地址(`process_makefile`所在的地址)

### 2. 使用 jar 包在命令行中使用各个功能

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
3. all: 保存make输出的所有内容(包括stdout和stderr), 如果进行了打开静默开关的抓取, make的所有内容会保存在这里
4. output: 对all里面结果过滤后的结果, 只有gcc命令以及文件夹切换的命令
5. outputParse: 保存对`output`文件转化后的各个命令
6. fileMap: 各种 .o .c 等文件的 **绝对路经** 与 .i 文件的 **绝对路径** 的对应关系
7. libMap: 各种动态静态链接库的 **绝对路径** 与 .o 文件的 **绝对路径**的对应关系
8. tasks.json: 最终结果输出的文件, 包括每个task有哪些文件, 行数统计等信息, 通过`cn.harry.captor.MakefileCapture.MakefileCapture.getTasksFromJson("location/of/tasks.json")`将json文件恢复为Tasks类


---

# TextCorresponderTool

这个工具是后面对于某个 .i 文件的某一行查询它对应的源文件具体在哪一行的工具

注意: 此工具处理的 .i 文件必须是加了 **-g** 参数生成的 .i 文件, 以此保证绝对路径的成功转换

## 使用

目前写了一个类 TextCorresponder 提供一些接口来调用这个工具

1. `boolean run(String filename)` 输入一个 .i 文件,处理并记录, 然后返回是否执行成功
2. `Stack<Pair> getSourcePath(int lineNumber)` 输入行号, 查询这行代码的源文件绝对路径和在源文件中的位置(一个Pair), 由于include是层层叠加的, 所以返回的是一个include的链
3. `RangeMap<Integer, Stack<Pair>> getRange()` 把经过`run()`处理得到的Range记录返回


