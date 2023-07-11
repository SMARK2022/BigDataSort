## 项目介绍

该项目是对一个总量为120G的数据文件集进行排序，比较排序速度。数据文件中包含了长度为15的随机英文字母字符串，以及换行符（'\n'）。项目需要将数据文件按照字符顺序进行排序，并将排序后的结果输出到以组号*100为起始的每隔100000行的字符串到一个文件中，文件名为"result+组号.txt"的格式。排序的正确率和完成时间将根据排名得分进行评估。


# LongArray类

## 构造函数
### LongArray()
- 描述：创建一个默认大小为4的LongArray对象，并初始化元素数量为0。
- 参数：无

### LongArray(int capacity)
- 描述：创建一个指定容量的LongArray对象，并初始化元素数量为0。
- 参数：
  - capacity：指定的容量大小。

## 方法
### void push(long value)
- 描述：将指定的long类型值添加到LongArray中。
- 参数：
  - value：要添加的long类型值。

### int size()
- 描述：返回当前LongArray中元素的数量。
- 参数：无
- 返回值：当前LongArray中元素的数量。

### void shrink()
- 描述：缩小LongArray的大小为当前元素数量。
- 参数：无

### long get(int index)
- 描述：获取指定索引处的元素值。
- 参数：
  - index：指定的索引。
- 返回值：指定索引处的元素值。

### void set(int index, long value)
- 描述：设置指定索引处的元素值。
- 参数：
  - index：指定的索引。
  - value：要设置的元素值。

### void sort()
- 描述：对LongArray进行排序，只排序前size个元素。
- 参数：无

### byte[] ExporttoBytes()
- 描述：将LongArray转换为字节数组。
- 参数：无
- 返回值：转换后的字节数组。

### static LongArray LoadfromBytes(byte[] bytes)
- 描述：将字节数组转换为LongArray。
- 参数：
  - bytes：要转换的字节数组。
- 返回值：转换后的LongArray对象。

### static LongArray merge(List<LongArray> arrays)
- 描述：将多个LongArray合并成一个LongArray。
- 参数：
  - arrays：要合并的LongArray列表。
- 返回值：合并后的LongArray对象。

### static List<LongArray> split(LongArray array, int chunkSize)
- 描述：将一个LongArray切分成多个LongArray。
- 参数：
  - array：要切分的LongArray。
  - chunkSize：切分的大小。
- 返回值：切分后的LongArray列表。

## 排序算法原理
该项目的排序算法使用的是Java语言内置的Arrays.sort()方法进行排序。该方法使用的是Timsort排序算法，速度实现较快。


# MainClientServer类

## 成员变量
- `BAR_WIDTH`: 进度条的宽度，类型为`int`，用于控制进度条的显示宽度。
- `NUM_STR_HIGH`: 归并段的数目，类型为`int`，用于控制归并段的数量。

- `filename_READ`: 读取的文件名，类型为`String`，用于记录要读取的文件名。
- `fileSize`: 文件大小，类型为`long`，用于记录读取的文件的大小。
- `count`: 读取的行数，类型为`long`，用于记录实际读取的行数。

- `mutex_data`: 数据互斥锁，类型为`Object`，用于在多线程环境下保护共享数据的访问。
- `mutex_display`: 显示互斥锁，类型为`Object`，用于在多线程环境下保护进度条显示的访问。
- `mutex_BucketData`: 桶数据互斥锁，类型为`Object`，用于在多线程环境下保护桶数据的访问。
- `mutex_Mergedata`: 归并数据互斥锁，类型为`Object`，用于在多线程环境下保护归并数据的访问。

- `N_Buckets`: 桶数据集合，类型为`List<int[]>`，用于存储桶数据。
- `Bucket_Merged`: 归并完成的桶编号，类型为`int`，用于记录归并阶段完成的桶编号。
- `Bucket_Dealt`: 处理完成的桶编号，类型为`int`，用于记录处理阶段完成的桶编号。
- `StartProcess`: 开始处理标志，类型为`boolean`，用于标记是否开始处理数据。

- `Thread_Bucket_DataBox`: 排序阶段线程数据块集合，类型为`List<List<LongArray>>`，用于存储排序阶段每个线程的数据块。
- `Bucket_Client_DataBox`: 收发阶段客户端数据块集合，类型为`List<List<LongArray>>`，用于存储收发阶段每个桶对应的客户端的数据块。

## 构造函数
### MainClientServer()
- 描述：创建一个MainClientServer对象。
- 参数：无

## 方法
### static void updateProgressBar(int progress, int total)
- 描述：更新进度条显示，根据当前进度和总进度计算得出百分比并显示进度条。
- 参数：
  - progress: 当前进度
  - total: 总进度

### static char[] converttostring(int ID_Bucket, long Low_Data_Str)
- 描述：将桶编号和低位数据转换成字符串，返回转换后的字符串。
- 参数：
  - ID_Bucket: 桶编号
  - Low_Data_Str: 低位数据
- 返回值：转换得到的字符数组

### static void readLines(long start, long rows, List<LongArray> data, long[] num)
- 描述：读取文件的指定行数数据，根据起始行数和行数读取文件中的数据。
- 参数：
  - start: 起始行数
  - rows: 读取的行数
  - data: 读取的数据存储在此列表中
  - num: 实际读取的行数存储在此数组中

### static void sortData(int thread_id, List<LongArray> data)
- 描述：对指定的数据集合进行排序。
- 参数：
  - thread_id: 线程ID
  - data: 待排序的数据集合

### static void merge(List<LongArray> data1, List<LongArray> data2, List<LongArray> merged_data_collection, int i)
- 描述：归并两个数据集合，并将结果放入合并后的数据集合中。
- 参数：
  - data1: 第一个数据集合
  - data2: 第二个数据集合
  - merged_data_collection: 归并后的数据集合
  - i: 归并线程ID

### static void ReadFile()
- 描述：多线程读取文件，获取文件的大小和行数，并将数据分块读入内存。

### static void SortBlocks()
- 描述：将排序阶段读入的多线程数据块进行块内排序。

### static void MultiMerge()
- 描述：对排序阶段排序后的数据进行多线程多路归并。

### static void SendData(String serverIP, int serverPort, int ChunkSize)
- 描述：将数据发送给指定的服务器端口。
- 参数：
  - serverIP: 目标主机IP
  - serverPort: 目标主机端口
  - ChunkSize: 分片大小

### static void mergeBucket(LongArray bucketA, LongArray bucketB, LongArray merged_bucket, int ID_Bucket)
- 描述：将两个桶中的数据进行归并。
- 参数：
  - bucketA: 第一个桶
  - bucketB: 第二个桶
  - merged_bucket: 归并后的桶
  - ID_Bucket: 桶编号

### static void ReceiveData(int ClientID, int listenPort, int NumBucketSize)
- 描述：接收数据，并将接收到的数据存储到对应的桶中，同时设定缓冲大小。
- 参数：
  - ClientID: 当前线程对应的客户端ID
  - listenPort: 当前线程监听端口
  - NumBucketSize: 当前客户端所要缓存的桶数

### static void TransferData(int ClientID, int NumBucketSize)
- 描述：将数据从线程数据块中转移到桶数据块中。
- 参数：
  - ClientID: 当前线程对应的客户端ID
  - NumBucketSize: 当前客户端所要缓存的桶数

### static void MultiMergeBucket()
- 描述：对接收到的桶数据进行多路归并。

### static void SaveData(String fileName, int start, int gap)
- 描述：将数据保存到文件。
- 参数：
  - fileName: 文件名
  - start: 起始位置
  - gap: 步长

### static void main(String[] args)
- 描述：项目的主方法，包含了整个排序过程的调用顺序和线程启动。


## 简要原理和作用

该项目使用多线程方式对数据进行排序。具体实现步骤如下：

1. 首先读取文件，并将数据分块读入内存，分块读取的数据存储在一个二维列表中。

2. 对每个数据块中的数据进行排序，使用Java内置的排序方法。

3. 将排序后的数据块进行多路归并排序，直到只剩下一个数据块。

4. 将归并得到的数据块发送给指定的服务器端口。

5. 服务器端接收数据，并将接收到的数据存储到对应的桶中。

6. 将数据从线程数据块中转移到桶数据块中。

7. 对桶数据块进行多路归并排序，直到所有桶的数据都归并完成。

8. 将归并得到的数据保存到文件中。

该项目采用多线程的方式进行数据处理和排序，以提高排序速度和效率。通过将数据分块读取和分块排序，减少了排序的时间复杂度，并且使用多路归并排序将各个数据块合并成最终的排序结果。同时，采用多线程的方式进行数据的接收和发送，以提高数据传输的效率。最后，将排序好的数据保存到文件中，方便后续的处理和使用。

该项目的作用是对大规模数据进行排序，并将排序结果保存到文件中。通过多线程和多路归并排序的方式，加快了排序的速度和效率，提高了大规模数据排序的处理能力。