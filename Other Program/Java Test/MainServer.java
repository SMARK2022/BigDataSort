import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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

  private long[] arr;
  private int size;

  public LongArray() {
    arr = new long[4];
    size = 0;
  }

  public LongArray(int capacity) {
    if (capacity == 0)
      arr = new long[4];
    else
      arr = new long[capacity];
    size = 0;
  }

  public void push(long value) {
    if (size == arr.length) {
      // 扩充数组大小
      arr = Arrays.copyOf(arr, (int) arr.length * 3 / 2);
    }
    arr[size++] = value;
  }

  public int size() {
    return size;
  }

  public void shrink() {
    arr = Arrays.copyOf(arr, size);
  }

  public long get(int index) {
    return arr[index];
  }

  public void set(int index, long value) {
    arr[index] = value;
  }

  public void sort() {
    Arrays.sort(arr, 0, size);
  }

  // 将LongArray转换为字节数组
  public byte[] ExporttoBytes() {
    int length = size * 8; // 每个long类型占8个字节
    byte[] bytes = new byte[length];
    for (int i = 0; i < size; i++) {
      long value = arr[i];
      for (int j = 0; j < 8; j++) {
        bytes[i * 8 + j] = (byte) (value >>> (56 - j * 8));
      }
    }
    return bytes;
  }

  // 将字节数组转换为LongArray
  public static LongArray LoadfromBytes(byte[] bytes) {
    int size = bytes.length / 8;
    LongArray longArray = new LongArray(size);
    for (int i = 0; i < size; i++) {
      long value = 0;
      for (int j = 0; j < 8; j++) {
        value |= (long) (bytes[i * 8 + j] & 0xFF) << (56 - j * 8);
      }
      longArray.push(value);
    }
    return longArray;
  }

  // 多个LongArray合并成一个LongArray
  public static LongArray merge(List<LongArray> arrays) {
    int totalSize = 0;
    for (LongArray array : arrays) {
      totalSize += array.size();
    }
    LongArray mergedArray = new LongArray(totalSize);
    for (LongArray array : arrays) {
      for (int i = 0; i < array.size(); i++) {
        mergedArray.push(array.get(i));
      }
    }
    return mergedArray;
  }

  // 将一个LongArray切分成多个LongArray
  public static List<LongArray> split(LongArray array, int chunkSize) {
    List<LongArray> splitArrays = new ArrayList<>();
    int size = array.size();
    int startIndex = 0;
    while (startIndex < size) {
      int endIndex = Math.min(startIndex + chunkSize, size);
      LongArray splitArray = new LongArray(endIndex - startIndex);
      for (int i = startIndex; i < endIndex; i++) {
        splitArray.push(array.get(i));
      }
      splitArrays.add(splitArray);
      startIndex = endIndex;
    }
    return splitArrays;
  }
}

class MainServer {

  private static final int BAR_WIDTH = 70;
  private static final int NUM_STR_HIGH = 676;

  private static int Bucket_Merged = -1;
  private static int Bucket_Dealt = -1;
  private static boolean StartProcess = false;

  private static List<int[]> N_Buckets = new ArrayList<>();

  private static final Object mutex_BucketData = new Object();
  private static final Object mutex_Mergedata = new Object();
  private static List<List<LongArray>> Client_data_str = new ArrayList<>(NUM_STR_HIGH);

  // private static boolean cmp(long a, long b) {
  // return a < b;
  // }

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

  // private static void sortData(int thread_id, List<LongArray> data) {
  // long sort_start = System.currentTimeMillis();
  // for (LongArray sublist : data) {
  // sublist.sort();
  // }
  // long sort_end = System.currentTimeMillis();
  // long sort_duration = sort_end - sort_start;
  // // synchronized (mutex_display) {
  // // System.out.println(
  // // "Thread" + thread_id + " 分段排序耗时: " + sort_duration + " 毫秒");
  // // }
  // }

  private static void mergeBucket(LongArray bucketA, LongArray bucketB, LongArray merged_bucket,
      int ID_Bucket) {
    // merged_data_collection.clear();
    // long merge_start = System.currentTimeMillis();

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

    // long merge_end = System.currentTimeMillis();
    // long merge_duration = merge_end - merge_start;
    // System.out.println("Thread" + ID_Bucket + " 归并排序耗时: " + merge_duration + "
    // 毫秒");

  }

  private static void ReceiveData(int ClientID, int listenPort, int NumBucketSize,
      List<List<LongArray>> DataBox) {
    double totalSize = 0; // 总数据量（单位为字节）
    List<LongArray> ChunkBuffer = new ArrayList<>();

    synchronized (mutex_BucketData) {
      while (Client_data_str.size() < NUM_STR_HIGH)
        Client_data_str.add(new ArrayList<>());
      while (N_Buckets.size() < ClientID + 1)
        N_Buckets.add(new int[] { -1 });
      for (int i_buck = 0; i_buck < NUM_STR_HIGH; i_buck++) {
        while (DataBox.get(i_buck).size() < ClientID + 1)
          DataBox.get(i_buck).add(new LongArray());
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

      // 开始计时测试
      long startTime = System.currentTimeMillis();

      int chunkSize = -1;
      int bucketNumber = -1;
      int index = -1;
      // 读取数据

      // 开始接收数据
      while (true) {
        // 读取发送前的信息
        // 解析接收到的数据
        chunkSize = inputStream.readInt();
        if (chunkSize == -1) {
          DataBox.get(bucketNumber).add(ClientID, LongArray.merge(ChunkBuffer));
          // System.out.println(listenPort + " 桶" + bucketNumber + "已装满");
          N_Buckets.get(ClientID)[0] = bucketNumber;
          break;
        }

        bucketNumber = inputStream.readInt();
        index = inputStream.readInt();
        if (index == 0 && bucketNumber > 0) {
          DataBox.get(bucketNumber - 1).set(ClientID, LongArray.merge(ChunkBuffer));
          ChunkBuffer = new ArrayList<>();
          // System.out.println(listenPort + " 桶" + bucketNumber + "已装满");
          N_Buckets.get(ClientID)[0] = bucketNumber - 1;
        }
        int bytesRead = 0;
        byte[] chunkData = new byte[chunkSize * 8];
        while ((bytesRead += inputStream.read(chunkData, bytesRead, chunkSize * 8 - bytesRead)) < 8 * chunkSize) {
          continue;
        }
        totalSize += chunkSize * 8;

        ChunkBuffer.add(index, LongArray.LoadfromBytes(chunkData));
        // System.out.println("写入一条");

        // 对接收到的数据进行处理

        // 发送握手消息

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

      // 输出测试结果
      long elapsedTime = endTime - startTime;
      double speed = (double) totalSize / elapsedTime * 1000 / 1024 / 1024;
      System.out.println(ClientID + "|接收端：数据接收完毕，总耗时：" + elapsedTime + "ms");
      System.out.println(ClientID + "|接收速度：" + speed + "MB/s");
      N_Buckets.get(ClientID)[0] = NUM_STR_HIGH;

      inputStream.close();
      outputStream.close();
      socket.close();
      serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void MultiMergeBucket() {
    int id_bucket;
    while (!StartProcess) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    long merge_start = System.currentTimeMillis();
    for (id_bucket = 0; id_bucket < NUM_STR_HIGH; id_bucket++) {
      while (true) {
        boolean run = false;
        for (int[] tmp_num : N_Buckets) {
          run = run || tmp_num[0] < id_bucket;
        }
        if (!run) {
          break;
        }
      }

      while (Client_data_str.get(id_bucket).size() > 1) {
        List<LongArray> new_bucket_data = new ArrayList<>();

        int newSize = Client_data_str.get(id_bucket).size();
        ExecutorService executor = Executors.newFixedThreadPool(newSize / 2);

        for (int i = 0; i < newSize; i += 2) {
          if (i + 1 < newSize) {
            final int index = i;
            executor.execute(() -> {
              LongArray merged = new LongArray();
              mergeBucket(Client_data_str.get(Bucket_Merged + 1).get(index),
                  Client_data_str.get(Bucket_Merged + 1).get(index + 1), merged, index);
              synchronized (mutex_Mergedata) {
                new_bucket_data.add(merged);
              }
            });
          } else {
            new_bucket_data.add(Client_data_str.get(Bucket_Merged + 1).get(i));
          }
        }

        executor.shutdown();

        try {
          executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        Client_data_str.set(Bucket_Merged + 1, new_bucket_data);

      }
      updateProgressBar(id_bucket + 1, NUM_STR_HIGH);
      // System.out.println(Bucket_Merged + 1 + " OK!");
      Bucket_Merged++;
    }

    long merge_end = System.currentTimeMillis();
    long merge_duration = merge_end - merge_start;
    System.out.println("归并完成耗时: " + merge_duration + " 毫秒");
  }

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
        // long startTime = System.currentTimeMillis();

        for (int i_buck = 0; i_buck < NUM_STR_HIGH; i_buck++) {

          while (Bucket_Merged < i_buck) {
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          List<LongArray> BucketChunks = LongArray.split(Client_data_str.get(i_buck).get(0), ChunkSize);
          // 开始计时测试
          for (int i = 0; i < BucketChunks.size(); i++) {
            byte[] sendData = BucketChunks.get(i).ExporttoBytes();

            // 发送前的信息
            int BucketNumber = i_buck; // 归并段编号
            int index = i; // 编号

            // 将发送前的信息合并成一个字节数组
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(byteStream);
            dataStream.writeInt(ChunkSize);
            dataStream.writeInt(BucketNumber);
            dataStream.writeInt(index);
            dataStream.write(sendData);
            byte[] sendBytes = byteStream.toByteArray();

            outputStream.write(sendBytes);

            // 进行握手
            String handshakeMsg = inputStream.readUTF();
            while (handshakeMsg != "c") {
              handshakeMsg = inputStream.readUTF();
            }
          }
          Bucket_Dealt++;
          Client_data_str.get(i_buck).set(0, null);

        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        dataStream.writeInt(-1);
        byte[] sendBytes = byteStream.toByteArray();
        outputStream.write(sendBytes);
        outputStream.close();
        socket.close();
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

  private static void SaveData(String fileName, int start, int gap) {
    while (!StartProcess) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    try {
      long Outputindex = start;
      long Sumbuckets = 0;

      FileWriter writer = new FileWriter(fileName);

      System.out.println("文件准备写入...");

      // long startTime = System.currentTimeMillis();

      for (int i_buck = 0; i_buck < NUM_STR_HIGH; i_buck++) {

        while (Bucket_Merged < i_buck) {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        for (; Outputindex - Sumbuckets < Client_data_str.get(i_buck).get(0).size(); Outputindex += gap) {
          // System.out.print(".");
          writer
              .write(converttostring(i_buck, Client_data_str.get(i_buck).get(0).get((int) (Outputindex - Sumbuckets))));
        }
        Sumbuckets += Client_data_str.get(i_buck).get(0).size();
        // 计算并保存

        Client_data_str.get(i_buck).set(0, null);
        Bucket_Dealt++;
      }
      writer.flush();
      writer.close(); // 关闭文件写入
      System.out.println("文件写入完成！");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {

    long all_start = System.currentTimeMillis();

    int Num_Ports = Integer.parseInt(args[0]);

    for (int i = 0; i < Num_Ports; i++) {
      final int threadIndex = i;
      Thread receiveThread = new Thread(() -> {
        ReceiveData(threadIndex + 1, 12345 + threadIndex, 6, Client_data_str);
      });
      receiveThread.start();
    }

    Thread MergeThread = new Thread(() -> {
      MultiMergeBucket();
    });
    // 启动线程
    MergeThread.start();

    Thread SaveThread = new Thread(() -> {
      SaveData("output.txt", 2200, 100000);
    });
    // 启动线程
    SaveThread.start();

    try {
      MergeThread.join();
      SaveThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    System.out.println("----------------------------------------------------------------------");

    long all_end = System.currentTimeMillis();
    long all_duration = all_end - all_start;

    System.out.println("总共运行耗时: " + all_duration + " 毫秒");

  }
}
