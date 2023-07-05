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

typedef struct Str
{
    char character[16];
} Str;

vector<Str> data_str; // �ṹ���������ڴ洢����

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

int main()
{
    // ��ʼ��ʱ
    auto start = std::chrono::steady_clock::now();

    // ���ļ�
    Str tmp;
    data_str.reserve(INIT_LINES);
    std::ifstream file_read("DATA.txt", ios::in);
    std::ofstream file_write("123.txt", ios::out);
    if (!file_read.is_open())
    {
        std::cout << "�޷����ļ�" << std::endl;
        return 0;
    }
    if (!file_write.is_open())
    {
        std::cout << "�޷����ļ�" << std::endl;
        return 0;
    }

    long long count = 0; // ���ڼ�¼��ȡ������

    // ���ж�ȡ�ļ�����
    std::string line;
    auto read_start = std::chrono::steady_clock::now(); // ��ȡ���ݽ׶ο�ʼ��ʱ
    while (std::getline(file_read, line))
    {
        // ���ַ������Ƶ��ṹ��������
        strncpy(tmp.character, line.c_str(), 16 * sizeof(char));
        data_str.push_back(tmp);

        count++;
    }
    auto read_end = std::chrono::steady_clock::now(); // ��ȡ���ݽ׶ν�����ʱ

    // �ر��ļ�
    file_read.close();

    auto sort_start = std::chrono::steady_clock::now(); // ����׶ο�ʼ��ʱ
    tim::timsort(data_str.begin(), data_str.end(), Cmp);
    // std::stable_sort(data_str.begin(), data_str.end(), Cmp);
    auto sort_end = std::chrono::steady_clock::now(); // ����׶ν�����ʱ

    // д�����ݽ׶ο�ʼ��ʱ
    auto write_start = std::chrono::steady_clock::now();
    // for (long long i = 0; i < count; i++)
    //     file_write << data_str[i].character << endl;
    // file_write.close();
    auto write_end = std::chrono::steady_clock::now(); // д�����ݽ׶ν�����ʱ

    // ����ÿ���׶εĺ�ʱ������Ϊ��λ��
    auto read_duration = std::chrono::duration_cast<std::chrono::milliseconds>(read_end - read_start).count();
    auto sort_duration = std::chrono::duration_cast<std::chrono::milliseconds>(sort_end - sort_start).count();
    auto write_duration = std::chrono::duration_cast<std::chrono::milliseconds>(write_end - write_start).count();

    // ���ÿ���׶εĺ�ʱ
    std::cout << "��ȡ���ݺ�ʱ: " << read_duration << " ����" << std::endl;
    std::cout << "����׶κ�ʱ: " << sort_duration << " ����" << std::endl;
    std::cout << "д�����ݺ�ʱ: " << write_duration << " ����" << std::endl;
    std::cout << "�ܹ������ʱ: " << read_duration + sort_duration + write_duration << " ����" << std::endl;

    std::cout << "�������� (n): " << count << std::endl;

    return 0;
}
