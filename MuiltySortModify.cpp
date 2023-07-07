#include <iostream>
#include <windows.h>
#include <string>
#include <vector>
#include <algorithm>
#include "tim/timsort.h"
#include <fstream>
#include <cstring>
#include <chrono>
#include <thread>
#include <mutex>
#include <condition_variable>

#define INIT_LINES 10000000
#define BAR_WIDTH 70
#define NUM_STR_HIGH 676
#define FILE_SEND_NAME "123.txt"

std::string filename_READ;

using namespace std;

std::mutex mutex_data;                             // 多线程数据锁
std::mutex mutex_display;                          // 多线程输出锁
vector<vector<vector<long long>>> thread_data_str; // 存放不同线程所处理的数据，三个维度分别为[线程][桶编号(前两位字符的26进制数字)][数据(以long long存储后13位)]
vector<long long> thread_data_num;                 // 存放着不同线程所处理数据的总量（现在是给进度条用的，可与进度条功能一同移去）

inline bool Cmp(long long a, long long b) // cmp比较函数，使用inline内联优化 (可以baidu)
{
    return a < b;
}

void updateProgressBar(int progress, int total) // 进度条更新函数(非必须)
{
    float progressPercentage = (float)progress / total;
    int progressWidth = BAR_WIDTH * progressPercentage;

    std::cout << "[";
    for (int i = 0; i < progressWidth; ++i)
    {
        std::cout << "=";
    }
    std::cout << ">";

    for (int i = progressWidth; i < BAR_WIDTH; ++i)
    {
        std::cout << " ";
    }

    std::cout << "] " << int(progressPercentage * 100) << "%\r";
    std::cout.flush();
}

void read_lines(long long start, long long rows, vector<vector<long long>> &data, long long &num)
// 按照读取起始位置、读取行数进行读取，将数据以[桶编号][数据读入次序]的格式储存在data这个引用变量中，并通过num实时输出进度
{
    ifstream file_read(filename_READ, ios::in);
    if (!file_read.is_open())
    {
        cout << "无法打开文件" << endl;
        return;
    }
    data.resize(NUM_STR_HIGH);
    string line;
    num = 0;
    file_read.seekg(start * 16, std::ios::beg);
    while (num < rows && getline(file_read, line))
    {
        long long tmp_LowStr = 0;
        for (int i = 2; i < 15; i++)
        {
            tmp_LowStr = tmp_LowStr * 26 + line[i] - 'a'; // 将后13位转换2进制编码
        }
        data[(line[0] - 'a') * 26 + line[1] - 'a'].push_back(tmp_LowStr); // 计算前两位所对应的桶编号数
        num++;                                                            // 进度+1
    }
    file_read.close();
}

void sort_data(int thread_id, vector<vector<long long>> &data) // 单纯是对timsort的封装，加了计时显示功能
{
    auto sort_start = chrono::steady_clock::now();
    for (int i = 0; i < NUM_STR_HIGH; i++)
        tim::timsort(data[i].begin(), data[i].end(), Cmp);
    auto sort_end = chrono::steady_clock::now(); // 写出数据阶段结束计时
    auto sort_duration = chrono::duration_cast<chrono::milliseconds>(sort_end - sort_start).count();
    mutex_display.lock();
    std::cout << "Thread" << thread_id << " 分段排序耗时: " << sort_duration << " 毫秒" << endl; // 为了避免两个线程同时打印，采用锁的方式
    mutex_display.unlock();
}

void merge(vector<vector<long long>> &data1, vector<vector<long long>> &data2, vector<vector<long long>> &merged_data_collection, int i)
// 二路归并函数，实现对两堆桶中相同桶编号的两个桶进行归并，最后生成一堆桶
{
    merged_data_collection.reserve(NUM_STR_HIGH);
    auto merge_start = chrono::steady_clock::now();
    for (int high = 0; high < NUM_STR_HIGH; high++) // 按桶编号开始进行归并
    {
        vector<long long> dataA, dataB;
        vector<long long> merged_data;
        long long i = 0, j = 0;
        dataA.swap(data1[high]); // 直接交换，避免复制和指针计算带来的额外内存与性能开销
        dataB.swap(data2[high]);

        while (i < dataA.size() && j < dataB.size()) // 正常归并流程
        {
            if (Cmp(dataA[i], dataB[j]))
            {
                merged_data.push_back(dataA[i]);
                i++;
            }
            else
            {
                merged_data.push_back(dataB[j]);
                j++;
            }
        }

        while (i < dataA.size())
        {
            merged_data.push_back(dataA[i]);
            i++;
        }
        while (j < dataB.size())
        {
            merged_data.push_back(dataB[j]);
            j++;
        }
        merged_data_collection.push_back(merged_data);
    }
    auto merge_end = chrono::steady_clock::now(); // 写出数据阶段结束计时
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    mutex_display.lock();
    std::cout << "Thread" << i << " 归并排序耗时: " << merge_duration << " 毫秒" << endl; // 同样有个打印锁
    mutex_display.unlock();
}

// 多路归并函数
void multi_merge(vector<vector<vector<long long>>> &thread_data_str, vector<vector<long long>> &merged_data) // 核心思想：将多线程生成的多堆数据通过两路归并进行多线程归并，以类似二叉树的形式归并成一堆
{
    auto merge_start = chrono::steady_clock::now();
    int thread_count = thread_data_str.size();

    // 递归归并
    while (thread_data_str.size() > 1) // 判断堆的数量是否大于1
    {
        vector<vector<vector<long long>>> new_temp_data;

        // 使用多线程进行归并
        vector<thread> merge_threads;
        for (int i = 0; i < thread_data_str.size(); i += 2)
        {
            if (i + 1 < thread_data_str.size())
            {
                // 创建一个新线程进行归并
                merge_threads.emplace_back([i, &thread_data_str, &new_temp_data]()
                                           {
                    vector<vector<long long>> merged;
                    merge(thread_data_str[i], thread_data_str[i + 1], merged,i);
                    // 使用互斥锁保护共享数据
                    mutex_data.lock();
                    new_temp_data.push_back(merged); mutex_data.unlock(); }); // 多线程进行二路归并，这里是一个lambda函数（无函数名的隐函数）
            }
            else
            {
                new_temp_data.push_back(thread_data_str[i]); // 末尾的奇数堆直接保留过来
            }
        }

        // 等待所有线程完成归并
        for (auto &thread : merge_threads)
        {
            thread.join();
        }
        std::cout << "----------------------------------------------------------------------" << std::endl; // 分隔符

        thread_data_str = new_temp_data;
    }

    // 最后剩下的数据即为最终的归并结果
    merged_data.swap(thread_data_str[0]);

    auto merge_end = chrono::steady_clock::now();
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    std::cout << "归并完成耗时: " << merge_duration << " 毫秒" << endl;
}

void save_data(const vector<vector<long long>> &data_collection) // 保存数据的函数，咱们应该用不上，排完序直接放内存里面，然后通过网络直接传给下一台主机
{
    auto write_start = chrono::steady_clock::now();
    ofstream file_write(FILE_SEND_NAME, ios::out);
    if (!file_write.is_open())
    {
        cout << "无法打开文件" << endl;
        return;
    }
    for (int high = 0; high < NUM_STR_HIGH; high++)
    {

        for (const auto &item : data_collection[high])
        {
            file_write << (high / 26) + 'a' << high % 26 + 'a' << endl;
        }
    }

    file_write.close();
    // 写出数据阶段的计时
    auto write_end = chrono::steady_clock::now(); // 写出数据阶段结束计时
                                                  // 计算每个阶段的耗时（毫秒为单位）
    auto write_duration = chrono::duration_cast<chrono::milliseconds>(write_end - write_start).count();

    // 输出每个阶段的耗时
    cout << "写出数据耗时: " << write_duration << " 毫秒" << endl;
}

int main(int argc, char *argv[])
{

    if (argc < 2) // 读取在命令提示行中被附加给程序的文本文件地址
    {
        std::cout << "请提供文件名" << std::endl;
        return 1; // 返回非零值表示程序异常退出
    }

    filename_READ = argv[1];
    // filename_READ = "DATA1G.txt";
    std::cout << "文件名: " << filename_READ << std::endl;

    auto all_start = chrono::steady_clock::now();  // 读取数据阶段结束计时
    auto read_start = chrono::steady_clock::now(); // 读取数据阶段开始计时

    std::ifstream file_read(filename_READ, ios::in);
    std::ofstream file_write(FILE_SEND_NAME, ios::out);
    long long count; // 用于记录读取的行数
    long long fileSize;

    if (file_read.is_open())
    {
        // 将文件指针移到文件末尾
        file_read.seekg(0, file_read.end);

        fileSize = file_read.tellg(); // 获取文件大小
        count = fileSize / 16;        // 通过获取文本总字符数计算总行数

        std::cout << "文件大小为：" << (double)fileSize / 1024 / 1024 / 1024 << "GB" << std::endl;
        std::cout << "文件行数为：" << count << std::endl;

        // 关闭文件
        file_read.close();
    }
    else
    {
        std::cout << "无法打开文件" << std::endl;
        return 0;
    }

    if (!file_write.is_open())
    {
        std::cout << "无法打开文件" << std::endl;
        return 0;
    }

    // 计算每个线程需要读取的行数
    int thread_count = thread::hardware_concurrency(); // 获取线程数
    long long lines_per_thread = count / thread_count; // 计算平均分配读取数量

    thread_data_str.resize(thread_count); // 提前创建好空间方便后续直接写入
    thread_data_num.resize(thread_count); // 注意区分resize和reverse哈！一个是直接填充空数组元素，一个是单纯预留内存空间

    // 读取数据阶段的多线程实现
    vector<thread> read_threads;
    for (int i = 0; i < thread_count; i++)
    {
        long long start = i * lines_per_thread;
        long long end = (i == (thread_count - 1)) ? count : lines_per_thread * (i + 1);

        read_threads.emplace_back(read_lines, start, end - start, ref(thread_data_str[i]), ref(thread_data_num[i])); // 多线程读入数据
    }

    while (true) // 这里是做那个进度条，如果不需要，可以直接替换为搞成只剩下for (auto &t : read_threads){t.join();}
    {
        std::chrono::milliseconds delay(250); // 延时时间为0.25秒

        long long tmp_read_lines = 0;
        for (int i = 0; i < thread_count; i++)
        {
            tmp_read_lines = tmp_read_lines + thread_data_num[i];
            // cout << i << " " << thread_data_num[i] << endl;
        }
        updateProgressBar(int((double)tmp_read_lines * 10000 / count), 10000);
        if (tmp_read_lines == count)
        {
            file_read.close();
            std::cout << std::endl;
            for (auto &t : read_threads)
            {
                t.join();
            }
            break;
        }
        std::this_thread::sleep_for(delay); // 延时0.25秒
    }

    auto read_end = chrono::steady_clock::now(); // 读取数据阶段结束计时
    auto read_duration = chrono::duration_cast<chrono::milliseconds>(read_end - read_start).count();
    cout << "读取数据耗时: " << read_duration << " 毫秒" << endl;
    std::cout << "----------------------------------------------------------------------" << std::endl;

    // 排序阶段的多线程实现
    vector<thread> sort_threads;
    for (int i = 0; i < thread_count; i++)
    {
        sort_threads.emplace_back(sort_data, i, ref(thread_data_str[i])); // 多线程完成数据排序
    }

    for (auto &t : sort_threads)
    {
        t.join(); // 等待线程完成
    }
    std::cout << "----------------------------------------------------------------------" << std::endl;

    // 归并排序阶段的多线程实现
    vector<vector<long long>> merged_data;
    multi_merge(thread_data_str, merged_data); // 最后运行一下封装好的多路归并函数
    std::cout << "----------------------------------------------------------------------" << std::endl;

    // 创建新的线程保存数据
    // thread save_thread(save_data, ref(merged_data));

    // // 等待保存数据线程结束
    // save_thread.join();
    auto all_end = chrono::steady_clock::now(); // 读取数据阶段结束计时
    auto all_duration = chrono::duration_cast<chrono::milliseconds>(all_end - all_start).count();

    cout << "总共处理耗时: " << all_duration << " 毫秒" << endl;
    cout << "总共处理速度: " << (double)fileSize / 1024 / 1024 * 1000 / all_duration << " MB/s" << endl;

    return 0;
}
