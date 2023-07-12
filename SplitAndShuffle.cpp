#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>
#include <random>
#include <thread>

void writeLinesToFile(const std::vector<std::string> &lines, const std::string &filename)
{
    std::ofstream outputFile(filename, std::ios::binary);
    if (!outputFile)
    {
        std::cout << "�޷������ļ���" << filename << std::endl;
        return;
    }

    std::string outputString;
    for (const std::string &line : lines)
    {
        outputString += line + "\n";
    }

    outputFile << outputString;

    outputFile.close();
}

void shuffleLinesInFile(const std::string &filename, const int numFiles)
{
    // ��ԭʼ�ļ����ж�ȡ
    std::ifstream inputFile(filename);
    if (!inputFile)
    {
        std::cout << "�޷����ļ���" << filename << std::endl;
        return;
    }

    // ��ȡԭʼ�ļ��е�ÿһ��
    std::vector<std::string> lines;
    std::string line;
    while (std::getline(inputFile, line))
    {
        lines.push_back(line);
    }
    inputFile.close();

    // �����еĴ���
    std::random_device rd;
    std::mt19937 g(rd());
    std::shuffle(lines.begin(), lines.end(), g);

    // ���о��ȷ�����С�ļ���
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

        // ����һ���̣߳�����������ת��Ϊһ���ַ�������д���ļ�
        threads.emplace_back(writeLinesToFile, linesPerFile, outputFilename);
    }

    // �ȴ������߳����
    for (std::thread &thread : threads)
    {
        thread.join();
    }

    std::cout << "�д����Ѵ��Ҳ��ɹ�������" << numFiles << "��С�ļ��С�" << std::endl;
}

int main(int argc, char *argv[])
{
    if (argc < 2) // ��ȡ��������ʾ���б����Ӹ�������ı��ļ���ַ
    {
        std::cout << "���ṩ�ļ���" << std::endl;
        return 1; // ���ط���ֵ��ʾ�����쳣�˳�
    }

    std::string filename = argv[1];
    int numFiles = atoi(argv[2]);
    // filename_READ = "DATA1G.txt";
    std::cout << "�ļ���: " << filename << std::endl;

    shuffleLinesInFile(filename, numFiles);

    return 0;
}
