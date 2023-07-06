#include <iostream>
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
#define FILE_READ_NAME "DATA.txt"
#define FILE_SEND_NAME "123.txt"

using namespace std;

typedef struct Str
{
    char character[16];
} Str;

vector<vector<Str>> thread_data_str; // 多个线程的结构体数组
mutex data_mutex;                    // 用于保护共享数据的互斥锁
condition_variable cv;               // 用于线程间的通信

short Cmp(Str a, Str b)
{
    for (short i = 0; i < 15; i++)
    {
        if (a.character[i] != b.character[i])
        {
            return a.character[i] < b.character[i];
        }
    }
    return 0;
}

void read_lines(int start, int end, vector<Str> &data)
{
    ifstream file_read(FILE_READ_NAME, ios::in);
    if (!file_read.is_open())
    {
        cout << "无法打开文件" << endl;
        return;
    }

    string line;
    int count = 0;
    while (getline(file_read, line))
    {
        if (count >= start && count < end)
        {
            Str tmp;
            strncpy(tmp.character, line.c_str(), 16 * sizeof(char));
            data.push_back(tmp);
        }
        count++;

        if (count >= end)
            break;
    }

    file_read.close();
}

void sort_data(int thread_id, vector<Str> &data)
{
    auto sort_start = chrono::steady_clock::now();
    tim::timsort(data.begin(), data.end(), Cmp);
    auto sort_end = chrono::steady_clock::now(); // 写出数据阶段结束计时
    auto sort_duration = chrono::duration_cast<chrono::milliseconds>(sort_end - sort_start).count();
    std::cout << "分段排序耗时: " << sort_duration << " 毫秒" << endl;
}
// 归并函数
void merge(vector<Str> &data1, vector<Str> &data2, vector<Str> &merged_data)
{
    int i = 0, j = 0;
    while (i < data1.size() && j < data2.size())
    {
        if (Cmp(data1[i], data2[j]))
        {
            merged_data.push_back(data1[i]);
            i++;
        }
        else
        {
            merged_data.push_back(data2[j]);
            j++;
        }
    }

    while (i < data1.size())
    {
        merged_data.push_back(data1[i]);
        i++;
    }

    while (j < data2.size())
    {
        merged_data.push_back(data2[j]);
        j++;
    }
}

// 多路归并函数
void multi_merge(vector<vector<Str>> &thread_data_str, vector<Str> &merged_data)
{
    auto merge_start = chrono::steady_clock::now();
    int thread_count = thread_data_str.size();
    vector<vector<Str>> temp_data(thread_count); // 存储每次归并的结果

    // 初始化temp_data为每个线程的排序后数据
    for (int i = 0; i < thread_count; i++)
    {
        temp_data[i] = thread_data_str[i];
        thread_data_str[i].clear();
        vector<Str>().swap(thread_data_str[i]);
    }
    thread_data_str.clear();
    vector<vector<Str>>().swap(thread_data_str);

    // 递归归并
    while (temp_data.size() > 1)
    {
        vector<vector<Str>> new_temp_data;

        // 每次取两个已排序的数据进行归并
        for (int i = 0; i < temp_data.size(); i += 2)
        {
            if (i + 1 < temp_data.size())
            {
                vector<Str> merged;
                merge(temp_data[i], temp_data[i + 1], merged);
                new_temp_data.push_back(merged);
            }
            else
            {
                new_temp_data.push_back(temp_data[i]);
            }
        }

        temp_data = new_temp_data;
    }

    // 最后剩下的数据即为最终的归并结果
    merged_data = temp_data[0];
    auto merge_end = chrono::steady_clock::now();
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    std::cout << "分段排序耗时: " << merge_duration << " 毫秒" << endl;
}

void save_data(const vector<Str> &data)
{
    auto write_start = chrono::steady_clock::now();
    ofstream file_write(FILE_SEND_NAME, ios::out);
    if (!file_write.is_open())
    {
        cout << "无法打开文件" << endl;
        return;
    }

    // for (const auto &item : data)
    // {
    //     file_write << item.character << endl;
    // }

    file_write.close();
    // 写出数据阶段的计时
    auto write_end = chrono::steady_clock::now(); // 写出数据阶段结束计时
                                                  // 计算每个阶段的耗时（毫秒为单位）
    auto write_duration = chrono::duration_cast<chrono::milliseconds>(write_end - write_start).count();

    // 输出每个阶段的耗时
    cout << "写出数据耗时: " << write_duration << " 毫秒" << endl;
}

int main()
{
    // 开始计时
    auto start = chrono::steady_clock::now();

    // 打开文件
    ifstream file_read(FILE_READ_NAME, ios::in);
    ofstream file_write(FILE_SEND_NAME, ios::out);
    if (!file_read.is_open())
    {
        cout << "无法打开文件" << endl;
        return 0;
    }
    if (!file_write.is_open())
    {
        cout << "无法打开文件" << endl;
        return 0;
    }

    long long count = 0; // 用于记录读取的行数

    // 逐行读取文件内容
    string line;
    auto read_start = chrono::steady_clock::now(); // 读取数据阶段开始计时
    while (getline(file_read, line))
    {
        count++;
    }
    auto read_end = chrono::steady_clock::now(); // 读取数据阶段结束计时
    // 关闭文件
    file_read.close();

    auto read_duration = chrono::duration_cast<chrono::milliseconds>(read_end - read_start).count();
    cout << "读取数据耗时: " << read_duration << " 毫秒" << endl;

    // 计算每个线程需要读取的行数
    int thread_count = thread::hardware_concurrency();
    int lines_per_thread = count / thread_count;

    thread_data_str.resize(thread_count);

    // 读取数据阶段的多线程实现
    vector<thread> read_threads;
    for (int i = 0; i < thread_count; i++)
    {
        int start = i * lines_per_thread;
        int end = (i == (thread_count - 1)) ? count : (i + 1) * lines_per_thread;

        read_threads.emplace_back(read_lines, start, end, ref(thread_data_str[i]));
    }

    for (auto &t : read_threads)
    {
        t.join();
    }

    // 排序阶段的多线程实现
    vector<thread> sort_threads;
    for (int i = 0; i < thread_count; i++)
    {
        sort_threads.emplace_back(sort_data, i, ref(thread_data_str[i]));
    }

    for (auto &t : sort_threads)
    {
        t.join();
    }

    // 归并排序阶段的多线程实现
    vector<Str> merged_data;
    multi_merge(thread_data_str, merged_data);

    // 创建新的线程保存数据
    thread save_thread(save_data, ref(merged_data));

    // 等待保存数据线程结束
    save_thread.join();

    // cout << "总共处理耗时: " << read_duration + sort_duration + write_duration << " 毫秒" << endl;

    cout << "数据行数 (n): " << count << endl;

    return 0;
}
