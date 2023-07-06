#include <iostream>
#include <stdio.h>
#include <string>
#include <cstdlib>
#include <ctime>
#include <thread>
#include <vector>
#include <mutex>

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

    for (int i = 0; i < length; i++)
    {
        int index = rand() % (sizeof(alphabet) - 1);
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
        free(lines);
        std::cout << " OK" << std::endl;
        mtx.unlock(); // 不加锁就注释掉
    }
}

int main()
{
    srand(time(0));

    if (!(outputFile = fopen("DATA16G.txt", "w")))
    {
        std::cerr << "Failed to open file." << std::endl;
        return 1;
    }

    int numLines = 1610612736;
    int stringLength = 15;
    int numThreads = std::thread::hardware_concurrency(); // Get the number of hardware threads

    std::vector<std::thread> threads;
    for (int i = 0; i < numThreads; i++)
    {
        threads.emplace_back(generateAndWriteRandomStrings, numLines / numThreads, stringLength, i);
    }

    // Wait for all threads to finish
    for (auto &thread : threads)
    {
        thread.join();
    }

    fclose(outputFile);

    return 0;
}
