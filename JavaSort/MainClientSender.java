import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class LongArray {

  private long[] arr; // 存储long类型的数组
  private int size; // 当前数组中元素的数量

  public LongArray() {
    arr = new long[4]; // 默认初始化为长度为4的数组
    size = 0; // 初始时没有元素
  }

  public LongArray(int capacity) {
    if (capacity == 0)
      arr = new long[4]; // 如果指定容量为0，则默认初始化为长度为4的数组
    else
      arr = new long[capacity]; // 否则初始化为指定容量的数组
    size = 0; // 初始时没有元素
  }

  public void push(long value) {
    if (size == arr.length) {
      // 扩充数组大小
      arr = Arrays.copyOf(arr, (int) arr.length * 3 / 2); // 扩充数组大小为当前长度的1.5倍
    }
    arr[size++] = value; // 添加元素并增加数组大小
  }

  public int size() {
    return size; // 返回当前数组中元素的数量
  }

  public void shrink() {
    arr = Arrays.copyOf(arr, size); // 缩小数组大小为当前元素数量
  }

  public long get(int index) {
    return arr[index]; // 获取指定索引处的元素
  }

  public void set(int index, long value) {
    arr[index] = value; // 设置指定索引处的元素值
  }

  public void sort() {
    Arrays.sort(arr, 0, size); // 对数组进行排序，只排序前size个元素
  }

  // 将LongArray转换为字节数组
  public byte[] ExporttoBytes() {
    int length = size * 8; // 每个long类型占8个字节
    byte[] bytes = new byte[length]; // 创建字节数组
    for (int i = 0; i < size; i++) {
      long value = arr[i];
      for (int j = 0; j < 8; j++) {
        bytes[i * 8 + j] = (byte) (value >>> (56 - j * 8)); // 将long类型的每个字节分别存储到字节数组中
      }
    }
    return bytes; // 返回字节数组
  }

  // 将字节数组转换为LongArray
  public static LongArray LoadfromBytes(byte[] bytes) {
    int size = bytes.length / 8; // 计算字节数组中能够转换成的long类型元素数量
    LongArray longArray = new LongArray(size); // 创建指定大小的LongArray对象
    for (int i = 0; i < size; i++) {
      long value = 0;
      for (int j = 0; j < 8; j++) {
        value |= (long) (bytes[i * 8 + j] & 0xFF) << (56 - j * 8); // 将字节数组中的每个字节转换成long类型，并将其合并成long类型的元素值
      }
      longArray.push(value); // 将转换后的long类型元素添加到LongArray中
    }
    return longArray; // 返回转换后的LongArray对象
  }

  // 多个LongArray合并成一个LongArray
  public static LongArray merge(List<LongArray> arrays) {
    int totalSize = 0; // 合并后的总大小
    for (LongArray array : arrays) {
      totalSize += array.size(); // 计算所有LongArray的元素总数量
    }
    LongArray mergedArray = new LongArray(totalSize); // 创建合并后的LongArray对象
    for (LongArray array : arrays) {
      for (int i = 0; i < array.size(); i++) {
        mergedArray.push(array.get(i)); // 将所有LongArray的元素依次添加到合并后的LongArray中
      }
    }
    return mergedArray; // 返回合并后的LongArray对象
  }

  // 将一个LongArray切分成多个LongArray
  public static List<LongArray> split(LongArray array, int chunkSize) {
    List<LongArray> splitArrays = new ArrayList<>(); // 存储切分后的LongArray列表
    int size = array.size(); // 原始LongArray的元素数量
    int startIndex = 0; // 切分起始索引
    while (startIndex < size) {
      int endIndex = Math.min(startIndex + chunkSize, size); // 计算切分结束索引，确保不越界
      LongArray splitArray = new LongArray(endIndex - startIndex); // 创建切分后的LongArray对象
      for (int i = startIndex; i < endIndex; i++) {
        splitArray.push(array.get(i)); // 将原始LongArray的元素依次添加到切分后的LongArray中
      }
      splitArrays.add(splitArray); // 将切分后的LongArray添加到列表中
      startIndex = endIndex; // 更新下一次切分的起始索引
    }
    return splitArrays; // 返回切分后的LongArray列表
  }
}

/**
 * 主客户端服务器类
 */
class MainClientServer {

  // 进度条宽度
  private static final int BAR_WIDTH = 70;
  // 归并段数目
  private static final int NUM_STR_HIGH = 676;

  // 读取的文件名
  private static String filename_READ;
  // 文件大小
  private static long fileSize;
  // 读取的行数
  private static long count;

  // 数据互斥锁
  private static final Object mutex_data = new Object();
  // 显示互斥锁
  private static final Object mutex_display = new Object();
  // 桶数据互斥锁
  private static final Object mutex_BucketData = new Object();
  // 归并数据互斥锁
  private static final Object mutex_Mergedata = new Object();

  // 桶数据集合
  private static List<int[]> N_Buckets = new ArrayList<>();
  // 归并完成的桶编号
  private static int Bucket_Merged = -1;
  // 处理完成的桶编号
  private static int Bucket_Dealt = -1;
  // 开始处理标志
  private static boolean StartProcess = false;

  // 排序阶段线程数据块集合，下标格式为[线程数编号][桶编号][数据项编号]
  private static List<List<LongArray>> Thread_Bucket_DataBox;
  // 收发阶段客户端数据块集合，下标格式为[桶编号][客户端编号][数据项编号]
  private static List<List<LongArray>> Bucket_Client_DataBox = new ArrayList<>(NUM_STR_HIGH);

  /**
   * 更新进度条
   *
   * @param progress 当前进度
   * @param total    总进度
   */
  private static void updateProgressBar(int progress, int total) {
    float progressPercentage = (float) progress / total;
    int progressWidth = (int) (BAR_WIDTH * progressPercentage);

    System.out.print("[");
    for (int i = 0; i < progressWidth; ++i) {
      System.out.print("=");
    }
    System.out.print(">");

    for (int i = progressWidth; i < BAR_WIDTH; ++i) {
      System.out.print(" ");
    }

    System.out.print("] " + (int) (progressPercentage * 100) + "%\r");
    System.out.flush();
  }

  /**
   * 将桶编号和低位数据转换成字符串
   *
   * @param ID_Bucket    桶编号
   * @param Low_Data_Str 低位数据
   * @return 转换得到的字符串
   */
  private static char[] converttostring(int ID_Bucket, long Low_Data_Str) {
    char[] tmp_string = new char[16];
    tmp_string[0] = (char) ('a' + ID_Bucket / 26);
    tmp_string[1] = (char) ('a' + ID_Bucket % 26);
    for (int i = 14; i > 1; i--) {
      tmp_string[i] = (char) ('a' + Low_Data_Str % 26);
      Low_Data_Str /= 26;
    }
    tmp_string[15] = '\n';
    return tmp_string;
  }

  /**
   * 读取文件的指定行数数据
   *
   * @param start 起始行数
   * @param rows  读取的行数
   * @param data  读取的数据
   * @param num   读取的行数
   * @throws IOException 读取文件异常
   */
  private static void readLines(
      long start,
      long rows,
      List<LongArray> data,
      long[] num) throws IOException {

    BufferedReader fileRead = new BufferedReader(new FileReader(filename_READ));

    for (int i = 0; i < NUM_STR_HIGH; i++) {
      data.add(new LongArray((int) (4 + rows / NUM_STR_HIGH / 2)));
    }
    String line;
    long tmp_LowStr = 0;
    num[0] = 0;
    fileRead.skip(start * 16);
    while (num[0] < rows) {
      line = fileRead.readLine();
      tmp_LowStr = 0;
      for (int i = 2; i < 15; i++) {
        tmp_LowStr = tmp_LowStr * 26 + line.charAt(i) - 'a';
      }
      data.get((line.charAt(0) - 'a') * 26 + line.charAt(1) - 'a').push(tmp_LowStr);
      num[0]++;
    }
    for (int i = 0; i < NUM_STR_HIGH; i++) {
      data.get(i).shrink();
    }
    fileRead.close();
  }

  /**
   * 对数据进行排序
   *
   * @param thread_id 线程ID
   * @param data      排序的数据集合
   */
  private static void sortData(int thread_id, List<LongArray> data) {
    long sort_start = System.currentTimeMillis();
    for (LongArray sublist : data) {
      sublist.sort();
    }
    long sort_end = System.currentTimeMillis();
    long sort_duration = sort_end - sort_start;
    synchronized (mutex_display) {
      System.out.println(
          "Thread" + thread_id + " 分段排序耗时: " + sort_duration + " 毫秒");
    }
  }

  /**
   * 合并数据
   *
   * @param data1                  数据1
   * @param data2                  数据2
   * @param merged_data_collection 合并后的数据集合
   * @param i                      合并线程ID
   */
  private static void merge(List<LongArray> data1, List<LongArray> data2, List<LongArray> merged_data_collection,
      int i) {
    merged_data_collection.clear();
    long merge_start = System.currentTimeMillis();
    LongArray dataA, dataB, merged_data;
    for (int high = 0; high < NUM_STR_HIGH; high++) {
      dataA = data1.get(high);
      data1.set(high, null);
      dataB = data2.get(high);
      data2.set(high, null);

      int i1 = 0, i2 = 0, sizeA = dataA.size(), sizeB = dataB.size();
      merged_data = new LongArray(sizeA + sizeB);
      while (i1 < sizeA && i2 < sizeB) {
        if (dataA.get(i1) < dataB.get(i2)) {
          merged_data.push(dataA.get(i1));
          i1++;
        } else {
          merged_data.push(dataB.get(i2));
          i2++;
        }
      }
      while (i1 < sizeA) {
        merged_data.push(dataA.get(i1));
        i1++;
      }
      while (i2 < sizeB) {
        merged_data.push(dataB.get(i2));
        i2++;
      }
      merged_data_collection.add(merged_data);
    }
    long merge_end = System.currentTimeMillis();
    long merge_duration = merge_end - merge_start;
    synchronized (mutex_display) {
      System.out.println(
          "Thread" + i + " 归并排序耗时: " + merge_duration + " 毫秒");
    }

  }

  /**
   * 读取文件
   */
  private static void ReadFile() {

    long read_start = System.currentTimeMillis();
    List<long[]> thread_data_num;
    File file_read = new File(filename_READ);
    if (file_read.exists()) {
      fileSize = file_read.length();
      count = fileSize / 16;
      System.out.println(
          "文件大小为：" + (double) fileSize / 1024 / 1024 / 1024 + "GB");
      System.out.println("文件行数为：" + count);
    } else {
      System.out.println("无法打开文件");
      System.exit(1);
    }
    int numProcessors = Runtime.getRuntime().availableProcessors();
    Thread_Bucket_DataBox = new ArrayList<>(numProcessors);
    thread_data_num = new ArrayList<long[]>(numProcessors);

    long lines_per_thread = count / numProcessors;

    for (int i = 0; i < numProcessors; i++) {
      final int index = i;
      long start = i * lines_per_thread;
      long end = (i == (numProcessors - 1))
          ? count
          : lines_per_thread * (i + 1);

      Thread_Bucket_DataBox.add(new ArrayList<>());
      thread_data_num.add(new long[] { 0L });

      Thread thread = new Thread(() -> {
        try {
          readLines(
              start,
              end - start,
              Thread_Bucket_DataBox.get(index),
              thread_data_num.get(index));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      thread.start();
    }

    while (true) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      long tmp_read_lines = 0;
      for (long[] num : thread_data_num) {
        tmp_read_lines += num[0];
      }

      updateProgressBar((int) ((double) tmp_read_lines * 10000 / count), 10000);

      if (tmp_read_lines == count) {
        System.out.println();
        break;
      }
    }

    long read_end = System.currentTimeMillis();
    long read_duration = read_end - read_start;
    System.out.println("读取数据耗时: " + read_duration + " 毫秒");

  }

  /**
   * 对块进行排序
   */
  private static void SortBlocks() {
    List<Thread> sort_threads = new ArrayList<>();
    for (int i = 0; i < Thread_Bucket_DataBox.size(); i++) {
      final int index = i;
      Thread thread = new Thread(() -> sortData(index, Thread_Bucket_DataBox.get(index)));
      sort_threads.add(thread);
      thread.start();
    }

    for (Thread thread : sort_threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 多路归并
   */
  private static void MultiMerge() {
    long merge_start = System.currentTimeMillis();

    while (Thread_Bucket_DataBox.size() > 1) {
      List<List<LongArray>> new_temp_data = new ArrayList<>();

      int newSize = Thread_Bucket_DataBox.size();
      ExecutorService executor = Executors.newFixedThreadPool(newSize / 2);

      for (int i = 0; i < newSize; i += 2) {
        if (i + 1 < newSize) {
          final int index = i;
          executor.execute(() -> {
            List<LongArray> merged = new ArrayList<LongArray>(NUM_STR_HIGH);
            merge(Thread_Bucket_DataBox.get(index), Thread_Bucket_DataBox.get(index + 1), merged, index);
            synchronized (mutex_data) {
              new_temp_data.add(merged);
            }
          });
        } else {
          new_temp_data.add(Thread_Bucket_DataBox.get(i));
        }
      }

      executor.shutdown();

      try {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println(
          "----------------------------------------------------------------------");
      Thread_Bucket_DataBox = new_temp_data;

    }

    long merge_end = System.currentTimeMillis();
    long merge_duration = merge_end - merge_start;
    System.out.println("归并完成耗时: " + merge_duration + " 毫秒");
  }

  /**
   * 发送数据
   *
   * @param serverIP   目标主机IP
   * @param serverPort 目标主机端口
   * @param ChunkSize  分片大小
   */
  private static void SendData(String serverIP, int serverPort, int ChunkSize) {
    boolean connected = false;
    System.out.println("发送端：目标主机 " + serverIP);
    System.out.println("发送端：目标端口 " + serverPort);
    while (!connected) {
      try {
        Socket socket = new Socket(serverIP, serverPort);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        // 进行握手
        outputStream.writeUTF("HandShake!");

        // 进行握手
        String handshakeMsg = inputStream.readUTF();
        System.out.println("发送端：握手消息：" + handshakeMsg);

        // 连接成功，设置 connected 为 true，跳出循环
        connected = true;

        for (int id_Bucket = 0; id_Bucket < NUM_STR_HIGH; id_Bucket++) {
          List<LongArray> BucketChunks = LongArray.split(Thread_Bucket_DataBox.get(0).get(id_Bucket), ChunkSize);
          if (BucketChunks.size() == 0) {
            // 将发送前的信息合并成一个字节数组
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(byteStream);
            // 发送前的信息
            int chunkSize = 0;
            int BucketNumber = id_Bucket; // 归并段编号
            int index = 0; // 编号
            dataStream.writeInt(chunkSize);
            dataStream.writeInt(BucketNumber);
            dataStream.writeInt(index);
            byte[] sendBytes = byteStream.toByteArray();
            outputStream.write(sendBytes);
            while ((char) inputStream.read() != 'c') {
              continue;
            }
          }
          for (int i = 0; i < BucketChunks.size(); i++) {
            // System.out.println("发送端：发送一个Chunk");
            byte[] sendData = BucketChunks.get(i).ExporttoBytes();

            // 发送前的信息
            int chunkSize = BucketChunks.get(i).size();
            int BucketNumber = id_Bucket; // 归并段编号
            int index = i; // 编号

            // 将发送前的信息合并成一个字节数组
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(byteStream);
            dataStream.writeInt(chunkSize);
            dataStream.writeInt(BucketNumber);
            dataStream.writeInt(index);
            dataStream.write(sendData);
            byte[] sendBytes = byteStream.toByteArray();
            outputStream.write(sendBytes);

            // 确认是否进行下一个数据块
            while ((char) inputStream.read() != 'c') {
              continue;
            }
          }
          updateProgressBar(id_Bucket + 1, NUM_STR_HIGH);
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        dataStream.writeInt(-1);
        byte[] sendBytes = byteStream.toByteArray();
        outputStream.write(sendBytes);
        outputStream.close();
        socket.close();
        System.out.println();
        System.out.println("发送完成");

      } catch (UnknownHostException e) {
        e.printStackTrace();
      } catch (IOException e) {
        // 输出连接错误信息
        System.out.println("连接错误：" + e.getMessage());
        try {
          // 等待 1 秒后重试连接
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * 合并桶数据
   *
   * @param bucketA       桶A
   * @param bucketB       桶B
   * @param merged_bucket 合并的桶数据
   * @param ID_Bucket     桶编号
   */
  private static void mergeBucket(LongArray bucketA, LongArray bucketB, LongArray merged_bucket,
      int ID_Bucket) {
    int i1 = 0, i2 = 0, sizeA = bucketA.size(), sizeB = bucketB.size();
    while (i1 < sizeA && i2 < sizeB) {
      if (bucketA.get(i1) < bucketB.get(i2)) {
        merged_bucket.push(bucketA.get(i1));
        i1++;
      } else {
        merged_bucket.push(bucketB.get(i2));
        i2++;
      }
    }
    while (i1 < sizeA) {
      merged_bucket.push(bucketA.get(i1));
      i1++;
    }
    while (i2 < sizeB) {
      merged_bucket.push(bucketB.get(i2));
      i2++;
    }
  }

  /**
   * 接收数据
   *
   * @param ClientID      当前线程对应的客户端ID
   * @param listenPort    当前线程监听端口
   * @param NumBucketSize 当前客户端所要缓存的桶数
   */
  private static void ReceiveData(int ClientID, int listenPort, int NumBucketSize) {
    double totalSize = 0; // 总数据量（单位为字节）
    List<LongArray> ChunkBuffer = new ArrayList<>();

    synchronized (mutex_BucketData) {
      while (Bucket_Client_DataBox.size() < NUM_STR_HIGH)
        Bucket_Client_DataBox.add(new ArrayList<>());
      while (N_Buckets.size() < ClientID + 1)
        N_Buckets.add(new int[] { -1 });
      for (int id_Bucket = 0; id_Bucket < NUM_STR_HIGH; id_Bucket++) {
        while (Bucket_Client_DataBox.get(id_Bucket).size() < ClientID + 1)
          Bucket_Client_DataBox.get(id_Bucket).add(new LongArray());
      }
    }

    try {
      ServerSocket serverSocket = new ServerSocket(listenPort);
      System.out.println(ClientID + "|接收端：监听中");

      Socket socket = serverSocket.accept();
      StartProcess = true;

      System.out.println(ClientID + "|接收端：连接成功！");

      DataInputStream inputStream = new DataInputStream(socket.getInputStream());
      DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

      // 进行握手
      String handshakeMsg = inputStream.readUTF();
      System.out.println(ClientID + "|接收端：握手消息：" + handshakeMsg);
      // 进行握手
      outputStream.writeUTF("HandShake!");

      // 开始计时
      long startTime = System.currentTimeMillis();

      int chunkSize = -1;
      int bucketNumber = -1;
      int index = -1;
      // 读取数据

      // 开始接收数据
      while (true) {
        // 解析接收到的数据头
        // 读取数据块的long总数量
        chunkSize = inputStream.readInt();
        if (chunkSize == -1) {
          // 如果读取到-1，表示数据接收完毕，将ChunkBuffer中的数据添加到相应的桶中，并更新桶号
          Bucket_Client_DataBox.get(bucketNumber).add(ClientID, LongArray.merge(ChunkBuffer));
          N_Buckets.get(ClientID)[0] = bucketNumber;
          break;
        }

        // 读取当前数据块所对应的桶编号
        bucketNumber = inputStream.readInt();
        index = inputStream.readInt();
        if (index == 0 && bucketNumber > 0) {
          // 如果index为0且桶号大于0，表示已经接收完前一个桶的数据，将ChunkBuffer中的数据添加到前一个桶中，并更新桶号
          Bucket_Client_DataBox.get(bucketNumber - 1).set(ClientID, LongArray.merge(ChunkBuffer));
          ChunkBuffer = new ArrayList<>();
          N_Buckets.get(ClientID)[0] = bucketNumber - 1;
        }
        int bytesRead = 0;
        byte[] chunkData = new byte[chunkSize * 8];
        while ((bytesRead += inputStream.read(chunkData, bytesRead, chunkSize * 8 - bytesRead)) < 8 * chunkSize) {// 循环直到读取到相应长度数据
          continue;
        }
        totalSize += chunkSize * 8;

        ChunkBuffer.add(index, LongArray.LoadfromBytes(chunkData));

        // 判断当前已接收数据量是否已达到缓冲长度
        while (N_Buckets.get(ClientID)[0] - Bucket_Dealt > NumBucketSize) {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        outputStream.write((byte) 'c');
      }
      long endTime = System.currentTimeMillis();

      // 附加输出输出测速结果
      long elapsedTime = endTime - startTime;
      double speed = (double) totalSize / elapsedTime * 1000 / 1024 / 1024;
      System.out.println(ClientID + "|接收端：数据接收完毕，总耗时：" + elapsedTime + "ms");
      System.out.println(ClientID + "|接收速度：" + speed + "MB/s");
      N_Buckets.get(ClientID)[0] = NUM_STR_HIGH;

      // 关闭数据流与socket连接
      inputStream.close();
      outputStream.close();
      socket.close();
      serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 本地数据转移
   *
   * @param ClientID      当前线程对应的客户端ID
   * @param NumBucketSize 当前客户端所要缓存的桶数
   */
  private static void TransferData(int ClientID, int NumBucketSize) {

    // 同步代码块，确保共享数据的线程安全性
    synchronized (mutex_BucketData) {
      // 如果Bucket_Client_DataBox的大小小于NUM_STR_HIGH，则向其添加空的ArrayList，以便存储数据
      while (Bucket_Client_DataBox.size() < NUM_STR_HIGH)
        Bucket_Client_DataBox.add(new ArrayList<>());
      // 如果N_Buckets的大小小于ClientID + 1，则向其添加一个新的int数组，用于记录当前客户端的接收完的桶编号号
      while (N_Buckets.size() < ClientID + 1)
        N_Buckets.add(new int[] { -1 });
      // 遍历所有的桶，确保Bucket_Client_DataBox中每个桶下方都能直接存放当前客户端的数据
      for (int id_Bucket = 0; id_Bucket < NUM_STR_HIGH; id_Bucket++) {
        while (Bucket_Client_DataBox.get(id_Bucket).size() < ClientID + 1)
          Bucket_Client_DataBox.get(id_Bucket).add(new LongArray());
      }
    }

    System.out.println(ClientID + "|转移端：待命中");

    // 等待StartProcess为true，表示数据接收端已经准备好
    while (!StartProcess) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println(ClientID + "|转移端：开始工作！");

    // 开始计时测试
    long startTime = System.currentTimeMillis();

    for (int id_Bucket = 0; id_Bucket < NUM_STR_HIGH; id_Bucket++) {

      Bucket_Client_DataBox.get(id_Bucket).set(ClientID, Thread_Bucket_DataBox.get(0).get(id_Bucket));// 完成从Thread_Bucket_DataBox的数据块向Bucket_Client_DataBox的迁移
      // 更新状态数据中当前客户端的已读取桶编号
      N_Buckets.get(ClientID)[0] = id_Bucket;

      // 进行缓冲段长度判断循环，以此限制缓冲数据Box中读取桶数的数量
      while (N_Buckets.get(ClientID)[0] - Bucket_Dealt > NumBucketSize) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    long endTime = System.currentTimeMillis();

    // 输出测试结果
    long elapsedTime = endTime - startTime;
    System.out.println(ClientID + "|转移端：数据转移完毕，总耗时：" + elapsedTime + "ms");
    N_Buckets.get(ClientID)[0] = NUM_STR_HIGH;

  }

  /**
   * 多路归并桶数据
   */
  private static void MultiMergeBucket() {

    // 等待StartProcess为true，表示数据接收端已经准备好
    while (!StartProcess) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // 开始计时测试
    long merge_start = System.currentTimeMillis();

    // 遍历所有的桶
    for (int id_Bucket = 0; id_Bucket < NUM_STR_HIGH; id_Bucket++) {
      // 循环判断是否所有客户端都已经接收到当前桶的数据
      while (true) {
        boolean run = false;
        for (int[] tmp_num : N_Buckets) {
          run = run || tmp_num[0] < id_Bucket;
        }
        if (!run) {
          break;
        }
      }

      // 对当前桶的数据进行多路归并，直到只剩下一个数据块
      while (Bucket_Client_DataBox.get(id_Bucket).size() > 1) {
        List<LongArray> new_bucket_data = new ArrayList<>();

        // 创建线程池，用于并发处理数据块的归并
        int newSize = Bucket_Client_DataBox.get(id_Bucket).size();
        ExecutorService executor = Executors.newFixedThreadPool(newSize / 2);

        // 每次取两个数据块进行归并，直到所有的数据块都归并完毕
        for (int i = 0; i < newSize; i += 2) {
          if (i + 1 < newSize) {
            final int index = i;
            executor.execute(() -> {
              LongArray merged = new LongArray();
              mergeBucket(Bucket_Client_DataBox.get(Bucket_Merged + 1).get(index),
                  Bucket_Client_DataBox.get(Bucket_Merged + 1).get(index + 1), merged, index);
              // 同步代码块，确保对new_bucket_data的访问线程安全
              synchronized (mutex_Mergedata) {
                new_bucket_data.add(merged);
              }
            });
          } else {
            new_bucket_data.add(Bucket_Client_DataBox.get(Bucket_Merged + 1).get(i));
          }
        }

        executor.shutdown();

        // 等待所有线程归并任务完成
        try {
          executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        // 更新Bucket_Client_DataBox中的数据块
        Bucket_Client_DataBox.set(Bucket_Merged + 1, new_bucket_data);
      }

      // 更新进度条，并记录已归并的桶数量
      updateProgressBar(id_Bucket + 1, NUM_STR_HIGH);
      Bucket_Merged++;
    }

    // 结束计时测试
    long merge_end = System.currentTimeMillis();
    long merge_duration = merge_end - merge_start;
    System.out.println("归并完成耗时: " + merge_duration + " 毫秒");
  }

  /**
   * 保存数据到文件
   *
   * @param fileName 文件名
   * @param start    起始位置
   * @param gap      步长
   */
  private static void SaveData(String fileName, int start, int gap) {
    // 等待StartProcess为true，表示数据接收端已经准备好
    while (!StartProcess) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      long Outputindex = start; // 输出索引，表示从当前位置开始输出数据
      long Sumbuckets = 0; // 已处理的桶中的数据总数

      FileWriter writer = new FileWriter(fileName); // 创建文件写入流

      System.out.println("文件准备写入...");

      // 遍历所有桶
      for (int id_Bucket = 0; id_Bucket < NUM_STR_HIGH; id_Bucket++) {
        // 等待桶归并的完成
        while (Bucket_Merged < id_Bucket) {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        // 将桶中的数据按照指定的步长写入文件
        for (; Outputindex - Sumbuckets < Bucket_Client_DataBox.get(id_Bucket).get(0).size(); Outputindex += gap) {
          writer.write(converttostring(id_Bucket,
              Bucket_Client_DataBox.get(id_Bucket).get(0).get((int) (Outputindex - Sumbuckets))));
        }
        Sumbuckets += Bucket_Client_DataBox.get(id_Bucket).get(0).size(); // 更新已处理的桶中的数据总数
        Bucket_Client_DataBox.get(id_Bucket).set(0, null); // 将当前桶中的数据置空，释放内存
        Bucket_Dealt++; // 更新已处理的桶的数量
      }

      writer.flush(); // 刷新文件写入缓冲区，确保数据写入到文件
      writer.close(); // 关闭文件写入流
      System.out.println("文件写入完成！");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("请提供文件名");
      System.exit(1);
    }

    // filename_READ = "F:\\Project\\BigDataSort/output_1.txt";
    // 获取输入参数
    filename_READ = args[0];
    String ip = args[1];
    int port = Integer.parseInt(args[2]);

    System.out.println("文件名: " + filename_READ);

    long all_start = System.currentTimeMillis();

    // 读取文件
    ReadFile();
    System.out.println(
        "----------------------------------------------------------------------");
    // 对数据进行分块排序
    SortBlocks();
    System.out.println(
        "----------------------------------------------------------------------");

    // 多路归并排序
    MultiMerge();
    System.out.println(
        "----------------------------------------------------------------------");

    long all_end = System.currentTimeMillis();
    long all_duration = all_end - all_start;

    // 输出处理耗时和处理速度
    System.out.println("总共处理耗时: " + all_duration + " 毫秒");
    System.out.println(
        "总共处理速度: " +
            (double) fileSize /
                1024 /
                1024 *
                1000 /
                all_duration
            +
            " MB/s");
    System.out.println(
        "----------------------------------------------------------------------");
    SendData(ip, port, 1024 * 64);
  }
}
