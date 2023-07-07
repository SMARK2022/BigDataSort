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
#define FILE_SEND_NAME "123.txt"

std::string filename_READ;

using namespace std;

typedef struct Str
{
    short High;
    long Low;
} Str;

std::mutex new_temp_data_mutex;
vector<vector<Str>> thread_data_str; // ����̵߳Ľṹ������

short Cmp(Str a, Str b)
{
    if (a.High != b.High)
        return a.High < b.High;
    else
        return a.Low < b.Low;
}

void read_lines(long long start, long long end, vector<Str> &data)
{
    ifstream file_read(filename_READ, ios::in);
    if (!file_read.is_open())
    {
        cout << "�޷����ļ�" << endl;
        return;
    }

    string line;
    long long count = 0;
    while (getline(file_read, line))
    {
        if (count >= start && count < end)
        {
            char tmp_char[15];
            Str tmp_Str;
            strncpy(tmp_char, line.c_str(), 15 * sizeof(char));
            tmp_Str.High = (tmp_char[0] - 'a') * 26 + tmp_char[1] - 'a';
            tmp_Str.Low = 0;
            for (int i = 2; i < 15; i++)
            {
                tmp_Str.Low = tmp_Str.Low * 26 + tmp_char[i] - 'a';
            }
            data.push_back(tmp_Str);
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
    auto sort_end = chrono::steady_clock::now(); // д�����ݽ׶ν�����ʱ
    auto sort_duration = chrono::duration_cast<chrono::milliseconds>(sort_end - sort_start).count();
    std::cout << "�ֶ������ʱ: " << sort_duration << " ����" << endl;
}
// �鲢����
void merge(vector<Str> &data1, vector<Str> &data2, vector<Str> &merged_data)
{
    auto merge_start = chrono::steady_clock::now();
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
    data1.clear();
    vector<Str>().swap(data1);

    while (j < data2.size())
    {
        merged_data.push_back(data2[j]);
        j++;
    }
    data2.clear();
    vector<Str>().swap(data2);

    auto merge_end = chrono::steady_clock::now(); // д�����ݽ׶ν�����ʱ
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    std::cout << "�鲢�����ʱ: " << merge_duration << " ����" << endl;
}

// ��·�鲢����
void multi_merge(vector<vector<Str>> &thread_data_str, vector<Str> &merged_data)
{
    auto merge_start = chrono::steady_clock::now();
    int thread_count = thread_data_str.size();

    // �ݹ�鲢
    while (thread_data_str.size() > 1)
    {
        vector<vector<Str>> new_temp_data;

        // ʹ�ö��߳̽��й鲢
        vector<thread> merge_threads;
        for (int i = 0; i < thread_data_str.size(); i += 2)
        {
            if (i + 1 < thread_data_str.size())
            {
                // ����һ�����߳̽��й鲢
                merge_threads.emplace_back([i, &thread_data_str, &new_temp_data]()
                                           {
                    vector<Str> merged;
                    merge(thread_data_str[i], thread_data_str[i + 1], merged);
                    // ʹ�û�����������������
                    lock_guard<mutex> lock(new_temp_data_mutex);
                    new_temp_data.push_back(merged); });
            }
            else
            {
                new_temp_data.push_back(thread_data_str[i]);
            }
        }

        // �ȴ������߳���ɹ鲢
        for (auto &thread : merge_threads)
        {
            thread.join();
        }

        thread_data_str = new_temp_data;
    }

    // ���ʣ�µ����ݼ�Ϊ���յĹ鲢���
    merged_data = thread_data_str[0];
    auto merge_end = chrono::steady_clock::now();
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    std::cout << "�鲢��ɺ�ʱ: " << merge_duration << " ����" << endl;
}

void save_data(const vector<Str> &data)
{
    auto write_start = chrono::steady_clock::now();
    ofstream file_write(FILE_SEND_NAME, ios::out);
    if (!file_write.is_open())
    {
        cout << "�޷����ļ�" << endl;
        return;
    }

    for (const auto &item : data)
    {
        file_write << (item.High / 26) + 'a' << item.High % 26 + 'a' << endl;
    }

    file_write.close();
    // д�����ݽ׶εļ�ʱ
    auto write_end = chrono::steady_clock::now(); // д�����ݽ׶ν�����ʱ
                                                  // ����ÿ���׶εĺ�ʱ������Ϊ��λ��
    auto write_duration = chrono::duration_cast<chrono::milliseconds>(write_end - write_start).count();

    // ���ÿ���׶εĺ�ʱ
    cout << "д�����ݺ�ʱ: " << write_duration << " ����" << endl;
}

int main(int argc, char *argv[])
{

    if (argc < 2)
    {
        std::cout << "Please provide a file name." << std::endl;
        return 1; // ���ط���ֵ��ʾ�����쳣�˳�
    }

    filename_READ = argv[1];

    std::cout << "File name: " << filename_READ << std::endl;

    auto all_start = chrono::steady_clock::now();  // ��ȡ���ݽ׶ν�����ʱ
    auto read_start = chrono::steady_clock::now(); // ��ȡ���ݽ׶ο�ʼ��ʱ

    std::ifstream file_read(filename_READ, ios::in);
    std::ofstream file_write(FILE_SEND_NAME, ios::out);
    long long count = 0; // ���ڼ�¼��ȡ������
    long long fileSize;

    if (file_read.is_open())
    {
        // ���ļ�ָ���Ƶ��ļ�ĩβ
        file_read.seekg(0, file_read.end);
        // ��ȡ�ļ���С
        fileSize = file_read.tellg();
        count = fileSize / 16;
        std::cout << "�ļ���СΪ��" << (double)fileSize / 1024 / 1024 / 1024 << "GB" << std::endl;
        std::cout << "�ļ�����Ϊ��" << count << std::endl;

        // �ر��ļ�
        file_read.close();
    }
    else
    {
        std::cout << "�޷����ļ�" << std::endl;
        return 0;
    }

    if (!file_write.is_open())
    {
        std::cout << "�޷����ļ�" << std::endl;
        return 0;
    }

    // ����ÿ���߳���Ҫ��ȡ������
    int thread_count = thread::hardware_concurrency();
    long long lines_per_thread = count / thread_count;

    thread_data_str.resize(thread_count);

    // ��ȡ���ݽ׶εĶ��߳�ʵ��
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

    auto read_end = chrono::steady_clock::now(); // ��ȡ���ݽ׶ν�����ʱ
    auto read_duration = chrono::duration_cast<chrono::milliseconds>(read_end - read_start).count();
    cout << "��ȡ���ݺ�ʱ: " << read_duration << " ����" << endl;

    // ����׶εĶ��߳�ʵ��
    vector<thread> sort_threads;
    for (int i = 0; i < thread_count; i++)
    {
        sort_threads.emplace_back(sort_data, i, ref(thread_data_str[i]));
    }

    for (auto &t : sort_threads)
    {
        t.join();
    }

    // �鲢����׶εĶ��߳�ʵ��
    vector<Str> merged_data;
    multi_merge(thread_data_str, merged_data);

    // �����µ��̱߳�������
    // thread save_thread(save_data, ref(merged_data));

    // // �ȴ����������߳̽���
    // save_thread.join();
    auto all_end = chrono::steady_clock::now(); // ��ȡ���ݽ׶ν�����ʱ
    auto all_duration = chrono::duration_cast<chrono::milliseconds>(all_end - all_start).count();

    cout << "�ܹ������ʱ: " << all_duration << " ����" << endl;
    cout << "�ܹ������ٶ�: " << (double)fileSize / 1024 / 1024 * 1000 / all_duration << " MB/s" << endl;

    return 0;
}
