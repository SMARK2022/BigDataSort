#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include "tim/timsort.h"
#include <fstream>
#include <cstring>
#include <chrono>

#define INIT_LINES 10000000

using namespace std;

vector<std::string> data_str; // 结构体数组用于存储数据

short Cmp(string a, string b)
{
    for (short i = 0; i < 15; i++)
    {
        if (a[i] != b[i])
        {
            return a[i] < b[i];
        }
    }
    return 0;
}

int main()
{
    // 开始计时
    auto start = std::chrono::steady_clock::now();

    // 打开文件
    string tmp;
    data_str.reserve(INIT_LINES);
    std::ifstream file_read("test.txt", ios::in);
    std::ofstream file_write("123.txt", ios::out);
    if (!file_read.is_open())
    {
        std::cout << "无法打开文件" << std::endl;
        return 0;
    }
    if (!file_write.is_open())
    {
        std::cout << "无法打开文件" << std::endl;
        return 0;
    }

    long long count = 0; // 用于记录读取的行数

    // 逐行读取文件内容
    std::string line;
    auto read_start = std::chrono::steady_clock::now(); // 读取数据阶段开始计时
    while (std::getline(file_read, line))
    {
        // 将字符串复制到结构体数组中
        data_str.push_back(line.c_str());

        count++;
    }
    auto read_end = std::chrono::steady_clock::now(); // 读取数据阶段结束计时

    // 关闭文件
    file_read.close();

    auto sort_start = std::chrono::steady_clock::now(); // 排序阶段开始计时
    // tim::timsort(data_str.begin(), data_str.end(), Cmp);
    std::stable_sort(data_str.begin(), data_str.end(), Cmp);
    auto sort_end = std::chrono::steady_clock::now(); // 排序阶段结束计时

    // 写出数据阶段开始计时
    auto write_start = std::chrono::steady_clock::now();
    // for (long long i = 0; i < count; i++)
    //     file_write << data_str[i] << endl;
    // file_write.close();
    auto write_end = std::chrono::steady_clock::now(); // 写出数据阶段结束计时

    // 计算每个阶段的耗时（毫秒为单位）
    auto read_duration = std::chrono::duration_cast<std::chrono::milliseconds>(read_end - read_start).count();
    auto sort_duration = std::chrono::duration_cast<std::chrono::milliseconds>(sort_end - sort_start).count();
    auto write_duration = std::chrono::duration_cast<std::chrono::milliseconds>(write_end - write_start).count();

    // 输出每个阶段的耗时
    std::cout << "读取数据耗时: " << read_duration << " 毫秒" << std::endl;
    std::cout << "排序阶段耗时: " << sort_duration << " 毫秒" << std::endl;
    std::cout << "写出数据耗时: " << write_duration << " 毫秒" << std::endl;
    std::cout << "总共处理耗时: " << read_duration + sort_duration + write_duration << " 毫秒" << std::endl;

    std::cout << "数据行数 (n): " << count << std::endl;

    return 0;
}
