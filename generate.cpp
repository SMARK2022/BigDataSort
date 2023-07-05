#include <iostream>
#include <fstream>
#include <string>
#include <cstdlib>
#include <ctime>

std::string generateRandomString(int length) {
    std::string randomString = "";
    static const char alphabet[] = "abcdefghijklmnopqrstuvwxyz";

    for (int i = 0; i < length; i++) {
        int index = rand() % (sizeof(alphabet) - 1);
        randomString += alphabet[index];
    }

    return randomString;
}

int main() {
    srand(time(0));

    std::ofstream outputFile("test.txt");
    if (!outputFile) {
        std::cerr << "Failed to open file." << std::endl;
        return 1;
    }

    int numLines = 10000000;
    int stringLength = 15;

    for (int i = 0; i < numLines; i++) {
        std::string randomString = generateRandomString(stringLength);
        outputFile << randomString << std::endl;
    }

    outputFile.close();

    return 0;
}
