import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Main {
    private static final int BAR_WIDTH = 70;
    private static final int NUM_STR_HIGH = 676;

    private static String filename_READ;

    private static final Object mutex_data = new Object();
    private static final Object mutex_display = new Object();
    private static List<List<List<Long>>> thread_data_str;

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

    private static void readLines(long start, long rows, List<List<Long>> data, long[] num) throws IOException {
        BufferedReader fileRead = new BufferedReader(new FileReader(filename_READ));
        data.clear();

        for (int i = 0; i < NUM_STR_HIGH; i++) {
            data.add(new ArrayList<>((int) rows / NUM_STR_HIGH));
        }
        String line;
        num[0] = 0;
        fileRead.skip(start * 16);
        while (num[0] < rows && (line = fileRead.readLine()) != null) {
            long tmp_LowStr = 0;
            for (int i = 2; i < 15; i++) {
                tmp_LowStr = tmp_LowStr * 26 + line.charAt(i) - 'a';
            }
            data.get((line.charAt(0) - 'a') * 26 + line.charAt(1) - 'a').add(tmp_LowStr);
            num[0]++;
        }
        fileRead.close();
    }

    private static void sortData(int thread_id, List<List<Long>> data) {
        long sort_start = System.currentTimeMillis();
        for (List<Long> sublist : data) {
            sublist.sort(Comparator.comparingLong(Long::longValue));
        }
        long sort_end = System.currentTimeMillis();
        long sort_duration = sort_end - sort_start;
        synchronized (mutex_display) {
            System.out.println("Thread" + thread_id + " 分段排序耗时: " + sort_duration + " 毫秒");
        }
    }

    private static void merge(List<List<Long>> data1, List<List<Long>> data2, List<List<Long>> merged_data_collection,
            int i) {
        merged_data_collection.clear();
        long merge_start = System.currentTimeMillis();
        for (int high = 0; high < NUM_STR_HIGH; high++) {
            List<Long> dataA = data1.get(high);
            List<Long> dataB = data2.get(high);
            List<Long> merged_data = new ArrayList<>(NUM_STR_HIGH);
            int i1 = 0, i2 = 0;
            while (i1 < dataA.size() && i2 < dataB.size()) {
                if (dataA.get(i1) < dataB.get(i2)) {
                    merged_data.add(dataA.get(i1));
                    i1++;
                } else {
                    merged_data.add(dataB.get(i2));
                    i2++;
                }
            }
            while (i1 < dataA.size()) {
                merged_data.add(dataA.get(i1));
                i1++;
            }
            while (i2 < dataB.size()) {
                merged_data.add(dataB.get(i2));
                i2++;
            }
            merged_data_collection.add(merged_data);
        }
        long merge_end = System.currentTimeMillis();
        long merge_duration = merge_end - merge_start;
        synchronized (mutex_display) {
            System.out.println("Thread" + i + " 归并排序耗时: " + merge_duration + " 毫秒");
        }
    }

    private static void multiMerge() {
        long merge_start = System.currentTimeMillis();

        while (thread_data_str.size() > 1) {
            List<List<List<Long>>> new_temp_data = new ArrayList<>();

            int newSize = thread_data_str.size();
            ExecutorService executor = Executors.newFixedThreadPool(newSize / 2);

            for (int i = 0; i < newSize; i += 2) {
                if (i + 1 < newSize) {
                    final int index = i;
                    executor.execute(() -> {
                        List<List<Long>> merged = new ArrayList<>(NUM_STR_HIGH);
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
            System.out.println("----------------------------------------------------------------------");

            thread_data_str = new_temp_data;
        }

        long merge_end = System.currentTimeMillis();
        long merge_duration = merge_end - merge_start;
        System.out.println("归并完成耗时: " + merge_duration + " 毫秒");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("请提供文件名");
            System.exit(1);
        }

        filename_READ = args[0];
        System.out.println("文件名: " + filename_READ);

        long all_start = System.currentTimeMillis();
        long read_start = System.currentTimeMillis();
        List<long[]> thread_data_num;
        long count = 0;
        long fileSize = 0;
        File file_read = new File(filename_READ);
        if (file_read.exists()) {
            fileSize = file_read.length();
            count = fileSize / 16;
            System.out.println("文件大小为：" + (double) fileSize / 1024 / 1024 / 1024 + "GB");
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
            long end = (i == (numProcessors - 1)) ? count : lines_per_thread * (i + 1);

            thread_data_str.add(new ArrayList<>());
            thread_data_num.add(new long[] { 0L });

            Thread thread = new Thread(() -> {
                try {
                    readLines(start, end - start, thread_data_str.get(index),
                            thread_data_num.get(index));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }

        while (true) {
            try {
                Thread.sleep(300);
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
        System.out.println("----------------------------------------------------------------------");

        List<Thread> sort_threads = new ArrayList<>();
        for (int i = 0; i < numProcessors; i++) {
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
        System.out.println("----------------------------------------------------------------------");

        multiMerge();
        System.out.println("----------------------------------------------------------------------");

        long all_end = System.currentTimeMillis();
        long all_duration = all_end - all_start;

        System.out.println("总共处理耗时: " + all_duration + " 毫秒");
        System.out.println("总共处理速度: " + (double) fileSize / 1024 / 1024 * 1000 / all_duration + " MB/s");
    }
}
