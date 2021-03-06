# ProcessMakefile

## ParameterHandler

这个类比较简单,主要就是两个功能

1. 获取命令行传入的参数 ,假设输出给我的(不用make处理的)参数 是这样的形式 -mk*=*, 单独进行处理(这部分还没写具体的参数,只是加了个过滤)
2. 将命令行传入的不需要这个工具处理的参数(一般是 make)提取出来,单独执行,并将输出重定向到文件里面(默认是 /ProcessLog/output )

## GetDetectedTasks

这个是整个工具的核心,包含了对 makefile 的输出的各种处理逻辑

主体逻辑就是逐行读入 `ParameterHandler` 输出的文件,并对每一行进行处理

每一行的处理分为五个判断

1. 判断这一条语句是不是 进入/退出 某个文件夹的语句
2. 判断这一条语句是不是编译得到某个中间文件( -c -S)
3. 判断这一条语句是不是一个生成静态链接库的情况 (ar xx)
4. 判断这一条语句是不是一个生成动态链接库的情况 (-shared)
5. 判断这一条语句是不是生成可执行文件的情况 

每个判断分别对应不同的处理逻辑

### 一. 进入/退出 文件夹

这个是最简单的处理逻辑, 就是用一个栈来存储

如果是进入某个文件夹的话就入栈
如果是退出某个文件夹的时候就出栈
其他需要执行的语句执行之前都要判断一下, 如果栈不空就先切换到栈顶的文件夹

为了方便文件夹的切换,栈里面存储文件夹的绝对地址

### 二. 编译得到中间文件  getProcessedFiles

这个处理逻辑主要就是要把得到中间文件(.s .o)的语句转化成得到预处理文件(.i)的语句并执行得到预处理文件

1. 先在整条语句后面加 " -E -O0" 表明执行完预处理阶段为止(以最后的 -E -c -S) 为准, 并且忽略各种优化
2. 判断这个函数有没有在输入参数指定输出,如果指定了,直接执行第7步
3. 判断这个语句有没有显示指定输出( -o ),如果指定了,直接执行第5步
4. 没有显示指定输出文件, gcc 是用输入源文件 加 .o .s 来默认生成输出的, 这里要提取出输入源文件名, 以便后面建立对应关系
5. 显示指定了输出文件, 直接根据 -o 提取处输出名称
6. 建立原来的输出(.s .o)和现在的输出(.i )的对应关系(用绝对地址),存在一个HashMap里面
7. 执行转化过的语句

预处理文件统一放入以行号命名的文件夹中

### 三. 处理静态链接库 getLibMap

这个处理逻辑主要针对一堆 .o 生成一个 .a/.ar 的情况

这种命令形式就是 ar -xxx xxx.a xxx.o

用一个或者多个空字符分开字符串, 得到字符串数组

第一个是ar 第二个是ar 的参数 第三个是输出的库文件 后面的全都是输入文件,统一放到一个字符串数组中

最后建立对应关系

### 四. 处理动态链接库 getSharedMap

这个处理逻辑也是针对一堆 .o 生成 一个库文件的情况, 但是命令的形式不同

这种命令形式是 cc/gcc -xxx1 -xxx2 -o out xxx.o

就是gcc 会有很多带 - 的输入参数,重点就是将这个过滤掉

1. 首先要把输入提取出来(-o 后面的就是), 然后在命令中删除输入, 避免后面提取输出的干扰
2. 去掉所有的 - 参数,要注意一些特殊情况的处理 ,比如 -MF -aux-info 等等
3. 最后剩下的就是所有的输入文件

以上就是处理 gcc 指令形式的方法,后面也会用到

最后输入输出建立关系

### 五. 处理生成可执行文件 getTasks

这个主要也是用了上面提到的 文件提取的办法

但是只去掉 -xxx 的参数可能还会有一些文件或者文件夹遗留, 但是肯定不会是前面生成的文件, 在前面生成的对应关系里面是找不到的, 所以不会影响

1. 找到输出文件, 保存之后然后替换为空
2. 找到输入文件列表
3. 如果输入文件里面有源文件的话 (.c ), 进行预处理得到 .i 文件, 存入目标文件夹
4. 将输入文件列表中的所有文件找到对应的 .i 文件 move到目标文件夹

输出统一放到 编号的 task文件夹
