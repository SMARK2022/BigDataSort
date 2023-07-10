import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

class Main {

  private static final int BAR_WIDTH = 70;
  private static final int NUM_STR_HIGH = 676;

  private static String filename_READ;
  private static long fileSize;
  private static long count;

  private static final Object mutex_data = new Object();
  private static final Object mutex_display = new Object();
  private static List<List<LongArray>> thread_data_str;

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

  private static void merge(List<LongArray> data1, List<LongArray> data2, List<LongArray> merged_data_collection,
      int i) {
    merged_data_collection.clear();
    long merge_start = System.currentTimeMillis();
    LongArray dataA, dataB, merged_data;
    for (int high = 0; high < NUM_STR_HIGH; high++) {
      // if (high > 1) {
      // System.out.println(data2.get(high));
      // }
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
    thread_data_str = new ArrayList<>(numProcessors);
    thread_data_num = new ArrayList<long[]>(numProcessors);

    long lines_per_thread = count / numProcessors;

    for (int i = 0; i < numProcessors; i++) {
      final int index = i;
      long start = i * lines_per_thread;
      long end = (i == (numProcessors - 1))
          ? count
          : lines_per_thread * (i + 1);

      thread_data_str.add(new ArrayList<>());
      thread_data_num.add(new long[] { 0L });

      Thread thread = new Thread(() -> {
        try {
          readLines(
              start,
              end - start,
              thread_data_str.get(index),
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
        // System.out.print(num[0]);
        // System.out.print(" ");
      }
      // System.out.println(tmp_read_lines);

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

  private static void SortBlocks() {
    List<Thread> sort_threads = new ArrayList<>();
    for (int i = 0; i < thread_data_str.size(); i++) {
      final int index = i;
      Thread thread = new Thread(() -> sortData(index, thread_data_str.get(index)));
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

  private static void MultiMerge() {
    long merge_start = System.currentTimeMillis();

    while (thread_data_str.size() > 1) {
      List<List<LongArray>> new_temp_data = new ArrayList<>();

      int newSize = thread_data_str.size();
      ExecutorService executor = Executors.newFixedThreadPool(newSize / 2);

      for (int i = 0; i < newSize; i += 2) {
        if (i + 1 < newSize) {
          final int index = i;
          executor.execute(() -> {
            List<LongArray> merged = new ArrayList<LongArray>(NUM_STR_HIGH);
            merge(thread_data_str.get(index), thread_data_str.get(index + 1), merged, index);
            synchronized (mutex_data) {
              new_temp_data.add(merged);
            }
          });
        } else {
          new_temp_data.add(thread_data_str.get(i));
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
      thread_data_str = new_temp_data;

    }

    long merge_end = System.currentTimeMillis();
    long merge_duration = merge_end - merge_start;
    System.out.println("归并完成耗时: " + merge_duration + " 毫秒");
  }

  private static void SendData(String serverIP, int serverPort) {
    int dataSize = 1024; // 每次发送的数据多少（单位为字节）

    try {
      Socket socket = new Socket(serverIP, serverPort);
      DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
      DataInputStream inputStream = new DataInputStream(socket.getInputStream());

      // 进行握手
      outputStream.writeUTF("握手消息");
      // long startTime = System.currentTimeMillis();

      for (int i_buck = 0; i_buck < NUM_STR_HIGH; i_buck++) {
        List<LongArray> BucketChunks = LongArray.split(thread_data_str.get(0).get(i_buck), dataSize);
        // 开始计时测试
        for (int i = 0; i < BucketChunks.size(); i++) {
          byte[] sendData = BucketChunks.get(i).ExporttoBytes();

          // 发送前的信息
          int ChunkSize = dataSize; // 数据块的大小
          int BucketNumber = i_buck; // 归并段编号
          int number = i; // 编号

          // 将发送前的信息合并成一个字节数组
          ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
          DataOutputStream dataStream = new DataOutputStream(byteStream);
          dataStream.writeInt(ChunkSize);
          dataStream.writeInt(BucketNumber);
          dataStream.writeInt(number);
          dataStream.write(sendData);
          byte[] sendBytes = byteStream.toByteArray();

          outputStream.write(sendBytes);

          // 进行握手
          String handshakeMsg = inputStream.readUTF();
          while (handshakeMsg != "c") {
            try {
              // 延时1秒
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            handshakeMsg = inputStream.readUTF();
          }
        }
      }

      // long endTime = System.currentTimeMillis();

      // 输出测试结果
      // long elapsedTime = endTime - startTime;
      // double speed = (double) dataSize * / elapsedTime / 1024 / 1024;
      // System.out.println("发送端：数据发送完毕，总耗时：" + elapsedTime + "ms");
      // System.out.println("发送速度：" + speed + "MB/s");

      outputStream.close();
      socket.close();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("请提供文件名");
      System.exit(1);
    }

    // filename_READ = "F:\\Project\\BigDataSort\\data01.txt";
    filename_READ = args[0];

    System.out.println("文件名: " + filename_READ);

    long all_start = System.currentTimeMillis();

    ReadFile();
    System.out.println(
        "----------------------------------------------------------------------");
    SortBlocks();
    System.out.println(
        "----------------------------------------------------------------------");

    MultiMerge();
    System.out.println(
        "----------------------------------------------------------------------");

    long all_end = System.currentTimeMillis();
    long all_duration = all_end - all_start;

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
    // for (int i = 0; i < NUM_STR_HIGH; i++) {
    // System.out.print(thread_data_str.get(0).get(i).get(0));
    // System.out.print(" ");
    // }
    // System.out.println(" ");
  }
}
