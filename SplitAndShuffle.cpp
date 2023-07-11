#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>
#include <random>
#include <thread>

void writeLinesToFile(const std::vector<std::string>& lines, const std::string& filename)
{
    std::ofstream outputFile(filename, std::ios::binary);
    if (!outputFile)
    {
        std::cout << "无法创建文件：" << filename << std::endl;
        return;
    }

    std::string outputString;
    for (const std::string& line : lines)
    {
        outputString += line + "\n";
    }

    outputFile << outputString;

    outputFile.close();
}

void shuffleLinesInFile(const std::string& filename, const int numFiles)
{
    // 打开原始文件进行读取
    std::ifstream inputFile(filename);
    if (!inputFile)
    {
        std::cout << "无法打开文件：" << filename << std::endl;
        return;
    }

    // 读取原始文件中的每一行
    std::vector<std::string> lines;
    std::string line;
    while (std::getline(inputFile, line))
    {
        lines.push_back(line);
    }
    inputFile.close();

    // 打乱行的次序
    std::random_device rd;
    std::mt19937 g(rd());
    std::shuffle(lines.begin(), lines.end(), g);

    // 将行均匀分配至小文件中
    int numLinesPerFile = lines.size() / numFiles;
    int remainder = lines.size() % numFiles;

    std::vector<std::thread> threads;

    for (int i = 0; i < numFiles; ++i)
    {
        int start = i * numLinesPerFile;
        int end = start + numLinesPerFile;
        if (i == numFiles - 1)
        {
            end += remainder;
        }

        std::vector<std::string> linesPerFile(lines.begin() + start, lines.begin() + end);
        std::string outputFilename = "output_" + std::to_string(i) + ".txt";

        // 创建一个线程，将多行数据转换为一个字符串，并写入文件
        threads.emplace_back(writeLinesToFile, linesPerFile, outputFilename);
    }

    // 等待所有线程完成
    for (std::thread& thread : threads)
    {
        thread.join();
    }

    std::cout << "行次序已打乱并成功分配至" << numFiles << "个小文件中。" << std::endl;
}

int main()
{
    std::string filename = "DATA12G.txt"; // 输入文件名
    int numFiles = 5;                   // 小文件数目

    shuffleLinesInFile(filename, numFiles);

    return 0;
}
