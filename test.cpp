#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include "tim/timsort.h"
#include <fstream>
#include <cstring>
#include <chrono>

#define MAX_LINES 1000000

using namespace std;

vector<std::string> data_str; // 结构体数组用于存储数据

short Cmp(std::string a, std::string b)
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

    // 打开文件
    data_str.reserve(MAX_LINES);
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
    while (std::getline(file_read, line))
    {
        if (count >= MAX_LINES)
        {
            break;
        }

        // 将字符串复制到结构体数组中
        data_str.push_back(line.c_str());
        // std::cout << tmp.character << std::endl;

        count++;
    }

    // 关闭文件
    file_read.close();



    // tim::timsort(data_str.begin(), data_str.end(), Cmp);
    std::stable_sort(data_str.begin(), data_str.end(), Cmp);


    // 开始计时
    auto start = std::chrono::steady_clock::now();

    for (long long i = 0; i < count; i++)
        file_write << data_str[i] << endl;
    file_write.close();


    // 结束计时
    auto end = std::chrono::steady_clock::now();
    // 计算代码块的执行时间（毫秒为单位）
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();

    // 输出执行时间
    std::cout << "代码块执行时间: " << duration << " 毫秒" << std::endl;

    std::cout << count << std::endl;
    std::cin >> count;

    return 0;
}
