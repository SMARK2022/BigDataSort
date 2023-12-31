#include <iostream>
#include <stdio.h>
#include <string>
#include <cstdlib>
#include <ctime>
#include <thread>
#include <vector>
#include <mutex>
#include <random>

std::mutex mtx;
FILE *outputFile;

typedef struct Str
{
    char character[16];
} Str;

Str Str_NULL = {'\0'};

Str generateRandomString(int length)
{
    Str randomString;
    static const char alphabet[] = "abcdefghijklmnopqrstuvwxyz";

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, sizeof(alphabet) - 2);

    for (int i = 0; i < length; i++)
    {
        int index = dis(gen);
        randomString.character[i] = alphabet[index];
    }
    randomString.character[length] = '\n';
    return randomString;
}

void generateAndWriteRandomStrings(int numLines, int stringLength, int threadId)
{
    int batchSize = 10485760;
    int numBatches = (numLines + batchSize - 1) / batchSize;

    for (int batch = 0; batch < numBatches; batch++)
    {
        int linesToGenerate = std::min(batchSize, numLines - batch * batchSize);

        // Create a vector to store the lines
        Str *lines = (Str *)malloc((linesToGenerate + 1) * sizeof(Str));

        for (int i = 0; i < linesToGenerate; i++)
        {
            // Add the random string to the vector
            lines[i] = generateRandomString(stringLength);
        }
        lines[linesToGenerate] = Str_NULL;

        // Lock the mutex before writing to the file
        mtx.lock(); // 不加锁就注释掉
        std::cout << "Writing " << linesToGenerate * 16 / 1024 / 1024 << "MB";

        // Write the lines to file
        // std::cout << lines[0].character;
        fprintf(outputFile, lines[0].character);
        fflush(outputFile);
        free(lines);
        std::cout << " OK" << std::endl;
        mtx.unlock(); // 不加锁就注释掉
    }
}

int main()
{
    srand(time(0));

    std::string filename;
    double N_GB;

    std::cout << "Enter the filename: ";
    std::cin >> filename;

    std::cout << "Enter the size in GB: ";
    std::cin >> N_GB;

    outputFile = fopen(filename.c_str(), "wb");

    if (!outputFile)
    {
        std::cerr << "Failed to open file." << std::endl;
        return 1;
    }

    long long numLines = N_GB * 1024 * 1024 * 1024 / 16;
    std::cout << "Lines: " << numLines << std::endl;

    int stringLength = 15;
    int numThreads = std::thread::hardware_concurrency();

    std::vector<std::thread> threads;

    for (int i = 0; i < numThreads; i++)
    {
        threads.emplace_back(generateAndWriteRandomStrings, i < numThreads - 1 ? numLines / numThreads : numLines - (numLines / numThreads) * (numThreads - 1), stringLength, i);
    }

    // Wait for all threads to finish
    for (auto &thread : threads)
    {
        thread.join();
    }
    fflush(outputFile);

    fclose(outputFile);

    return 0;
}
