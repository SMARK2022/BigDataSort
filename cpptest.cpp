#include <iostream>
#include <fstream>
#include <vector>
#include <windows.h>
using namespace std;

int main()
{
    ifstream file_read("123.txt", ios::in);
    if (!file_read.is_open())
    {
        cout << "无法打开文件" << endl;
        return 0;
    }
    string line;
    file_read.seekg(3, std::ios::beg);
    getline(file_read, line);
    std::cout << line << std::endl;

    return 0;
}
