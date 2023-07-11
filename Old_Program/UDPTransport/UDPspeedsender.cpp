#include <iostream>
#include <WinSock2.h>
#include <chrono>

#pragma comment(lib, "ws2_32.lib")

#define PORT 12345
#define IP_ADDRESS "10.250.234.28"
#define BUFFER_SIZE 1024
#define FILE_SIZE 1073741824  // 1MB

int main() {
    // 初始化Winsock
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        std::cerr << "Failed to initialize Winsock" << std::endl;
        return -1;
    }

    // 创建UDP套接字
    SOCKET sendingSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (sendingSocket == INVALID_SOCKET) {
        std::cerr << "Failed to create socket" << std::endl;
        WSACleanup();
        return -1;
    }

    // 设置目标地址和端口
    sockaddr_in receiverAddr;
    receiverAddr.sin_family = AF_INET;
    receiverAddr.sin_port = htons(PORT);
    receiverAddr.sin_addr.s_addr = inet_addr(IP_ADDRESS);

    // 发送握手信号
    char handshakeBuffer[BUFFER_SIZE];
    memset(handshakeBuffer, 'H', BUFFER_SIZE);  // 填充握手信号
    int result = sendto(sendingSocket, handshakeBuffer, BUFFER_SIZE, 0, (sockaddr*)&receiverAddr, sizeof(receiverAddr));
    if (result == SOCKET_ERROR) {
        std::cerr << "Failed to send handshake: " << WSAGetLastError() << std::endl;
        closesocket(sendingSocket);
        WSACleanup();
        return -1;
    }

    // 发送数据
    char* message = new char[BUFFER_SIZE];
    memset(message, 'A', BUFFER_SIZE);  // 填充数据
    auto start = std::chrono::high_resolution_clock::now();  // 开始时间

    for (int i = 0; i < FILE_SIZE / BUFFER_SIZE; i++) {
        result = sendto(sendingSocket, message, BUFFER_SIZE, 0, (sockaddr*)&receiverAddr, sizeof(receiverAddr));
        if (result == SOCKET_ERROR) {
            std::cerr << "Failed to send data: " << WSAGetLastError() << std::endl;
            break;
        }
    }

    auto end = std::chrono::high_resolution_clock::now();  // 结束时间
    double elapsedTime = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count() / 1000.0;  // 传输时间（秒）
    double transferSpeed = FILE_SIZE / (elapsedTime * 1024 * 1024);  // 传输速度（MB/s）
    std::cout << "Transfer speed: " << transferSpeed << " MB/s" << std::endl;

    // 关闭套接字
    closesocket(sendingSocket);
    WSACleanup();

    delete[] message;

    return 0;
}
