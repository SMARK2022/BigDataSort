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
#define NUM_STR_HIGH 676

std::string filename_READ;

using namespace std;

std::mutex mutex_data;                             // ���߳�������������ͬʱ�����ݽ���д�����
std::mutex mutex_display;                          // ���߳�����������ⲻͬ�߳��������໥����Ӱ��۲�
vector<vector<vector<long long>>> thread_data_str; // ��Ų�ͬ�߳������������ݣ�����ά�ȷֱ�Ϊ[�߳�][Ͱ���(ǰ��λ�ַ���26��������)][���ݱ��(��long long�洢��13λ)]
vector<long long> thread_data_num;                 // ����Ų�ͬ�߳����������ݵ������������Ǹ��������õģ��������������һͬ��ȥ��

inline bool Cmp(long long a, long long b) // cmp�ȽϺ�����ʹ��inline�����Ż� (����baidu)
{
    return a < b;
}

void read_lines(long long start, long long rows, vector<vector<long long>> &data, long long &num)
// ���ն�ȡ��ʼλ�á���ȡ�������ж�ȡ����������[Ͱ���][���ݶ������]�ĸ�ʽ������data������ñ����У���ͨ��numʵʱ�������
{
    ifstream file_read(filename_READ, ios::in);
    if (!file_read.is_open())
    {
        cout << "�޷����ļ�" << endl;
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
            tmp_LowStr = tmp_LowStr * 26 + line[i] - 'a'; // ����13λת��2���Ʊ���
        }
        data[(line[0] - 'a') * 26 + line[1] - 'a'].push_back(tmp_LowStr); // ����ǰ��λ����Ӧ��Ͱ�����
        num++;                                                            // ����+1
    }
    file_read.close();
}

void sort_data(int thread_id, vector<vector<long long>> &data) // �����Ƕ�timsort�ķ�װ�����˼�ʱ��ʾ����
{
    auto sort_start = chrono::steady_clock::now();
    for (int i = 0; i < NUM_STR_HIGH; i++)
        tim::timsort(data[i].begin(), data[i].end(), Cmp);
    auto sort_end = chrono::steady_clock::now(); // д�����ݽ׶ν�����ʱ
    auto sort_duration = chrono::duration_cast<chrono::milliseconds>(sort_end - sort_start).count();
    mutex_display.lock();
    std::cout << "Thread" << thread_id << " �ֶ������ʱ: " << sort_duration << " ����" << endl; // Ϊ�˱��������߳�ͬʱ��ӡ���������ķ�ʽ
    mutex_display.unlock();
}

void merge(vector<vector<long long>> &data1, vector<vector<long long>> &data2, vector<vector<long long>> &merged_data_collection, int i)
// ��·�鲢������ʵ�ֶ�����Ͱ����ͬͰ��ŵ�����Ͱ���й鲢���������һ��Ͱ
{
    merged_data_collection.reserve(NUM_STR_HIGH);
    auto merge_start = chrono::steady_clock::now();
    for (int high = 0; high < NUM_STR_HIGH; high++) // ��Ͱ��ſ�ʼ���й鲢
    {
        vector<long long> dataA, dataB;
        vector<long long> merged_data;
        long long i = 0, j = 0;
        dataA.swap(data1[high]); // ֱ�ӽ��������⸴�ƺ�ָ���������Ķ����ڴ������ܿ���
        dataB.swap(data2[high]);

        while (i < dataA.size() && j < dataB.size()) // �����鲢����
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
    auto merge_end = chrono::steady_clock::now(); // д�����ݽ׶ν�����ʱ
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    mutex_display.lock();
    std::cout << "Thread" << i << " �鲢�����ʱ: " << merge_duration << " ����" << endl; // ͬ���и���ӡ��
    mutex_display.unlock();
}

// ��·�鲢����
void multi_merge(vector<vector<vector<long long>>> &thread_data_str, vector<vector<long long>> &merged_data) // ����˼�룺�����߳����ɵĶ������ͨ����·�鲢���ж��̹߳鲢�������ƶ���������ʽ�鲢��һ��
{
    auto merge_start = chrono::steady_clock::now();
    int thread_count = thread_data_str.size();

    // �ݹ�鲢
    while (thread_data_str.size() > 1) // �ж϶ѵ������Ƿ����1
    {
        vector<vector<vector<long long>>> new_temp_data;

        // ʹ�ö��߳̽��й鲢
        vector<thread> merge_threads;
        for (int i = 0; i < thread_data_str.size(); i += 2)
        {
            if (i + 1 < thread_data_str.size())
            {
                // ����һ�����߳̽��й鲢
                merge_threads.emplace_back([i, &thread_data_str, &new_temp_data]()
                                           {
                    vector<vector<long long>> merged;
                    merge(thread_data_str[i], thread_data_str[i + 1], merged,i);
                    // ʹ�û�����������������
                    mutex_data.lock();
                    new_temp_data.push_back(merged); mutex_data.unlock(); }); // ���߳̽��ж�·�鲢��������һ��lambda�������޺���������������
            }
            else
            {
                new_temp_data.push_back(thread_data_str[i]); // ĩβ��������ֱ�ӱ�������
            }
        }

        // �ȴ������߳���ɹ鲢
        for (auto &thread : merge_threads)
        {
            thread.join();
        }
        std::cout << "----------------------------------------------------------------------" << std::endl; // �ָ���

        thread_data_str = new_temp_data;
    }

    // ���ʣ�µ����ݼ�Ϊ���յĹ鲢���
    merged_data.swap(thread_data_str[0]);

    auto merge_end = chrono::steady_clock::now();
    auto merge_duration = chrono::duration_cast<chrono::milliseconds>(merge_end - merge_start).count();
    std::cout << "�鲢��ɺ�ʱ: " << merge_duration << " ����" << endl;
}

int main(int argc, char *argv[])
{

    if (argc < 2) // ��ȡ��������ʾ���б����Ӹ�������ı��ļ���ַ
    {
        std::cout << "���ṩ�ļ���" << std::endl;
        return 1; // ���ط���ֵ��ʾ�����쳣�˳�
    }

    filename_READ = argv[1];
    // filename_READ = "DATA1G.txt";
    std::cout << "�ļ���: " << filename_READ << std::endl;

    auto all_start = chrono::steady_clock::now();  // ��ȡ���ݽ׶ν�����ʱ
    auto read_start = chrono::steady_clock::now(); // ��ȡ���ݽ׶ο�ʼ��ʱ

    std::ifstream file_read(filename_READ, ios::in);
    std::ofstream file_write(FILE_SEND_NAME, ios::out);
    long long count; // ���ڼ�¼��ȡ������
    long long fileSize;

    if (file_read.is_open())
    {
        // ���ļ�ָ���Ƶ��ļ�ĩβ
        file_read.seekg(0, file_read.end);

        fileSize = file_read.tellg(); // ��ȡ�ļ���С
        count = fileSize / 16;        // ͨ����ȡ�ı����ַ�������������

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
    int thread_count = thread::hardware_concurrency(); // ��ȡ�߳���
    long long lines_per_thread = count / thread_count; // ����ƽ�������ȡ����

    thread_data_str.resize(thread_count); // ��ǰ�����ÿռ䷽�����ֱ��д��
    thread_data_num.resize(thread_count); // ע������resize��reverse����һ����ֱ����������Ԫ�أ�һ���ǵ���Ԥ���ڴ�ռ�

    // ��ȡ���ݽ׶εĶ��߳�ʵ��
    vector<thread> read_threads;
    for (int i = 0; i < thread_count; i++)
    {
        long long start = i * lines_per_thread;
        long long end = (i == (thread_count - 1)) ? count : lines_per_thread * (i + 1);

        read_threads.emplace_back(read_lines, start, end - start, ref(thread_data_str[i]), ref(thread_data_num[i])); // ���̶߳�������
    }

    for (auto &t : read_threads)
    {
        t.join();
    }
    break;

    auto read_end = chrono::steady_clock::now(); // ��ȡ���ݽ׶ν�����ʱ
    auto read_duration = chrono::duration_cast<chrono::milliseconds>(read_end - read_start).count();
    cout << "��ȡ���ݺ�ʱ: " << read_duration << " ����" << endl;
    std::cout << "----------------------------------------------------------------------" << std::endl;

    // ����׶εĶ��߳�ʵ��
    vector<thread> sort_threads;
    for (int i = 0; i < thread_count; i++)
    {
        sort_threads.emplace_back(sort_data, i, ref(thread_data_str[i])); // ���߳������������
    }

    for (auto &t : sort_threads)
    {
        t.join(); // �ȴ��߳����
    }
    std::cout << "----------------------------------------------------------------------" << std::endl;

    // �鲢����׶εĶ��߳�ʵ��
    vector<vector<long long>> merged_data;
    multi_merge(thread_data_str, merged_data); // �������һ�·�װ�õĶ�·�鲢����
    std::cout << "----------------------------------------------------------------------" << std::endl;

    auto all_end = chrono::steady_clock::now(); // ��ȡ���ݽ׶ν�����ʱ
    auto all_duration = chrono::duration_cast<chrono::milliseconds>(all_end - all_start).count();

    cout << "�ܹ�������ʱ: " << all_duration << " ����" << endl;
    cout << "�ܹ������ٶ�: " << (double)fileSize / 1024 / 1024 * 1000 / all_duration << " MB/s" << endl;
    std::cout << "----------------------------------------------------------------------" << std::endl;

    for (int i = 0; i < NUM_STR_HIGH; i++)
    {
        std::cout << merged_data[i][0] << " ";
    }
    std::cout << std::endl;
    return 0;
}