#include <iostream>
#include <WinSock2.h>
#include <chrono>

#pragma comment(lib, "ws2_32.lib")

#define PORT 12345
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
    SOCKET receivingSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (receivingSocket == INVALID_SOCKET) {
        std::cerr << "Failed to create socket" << std::endl;
        WSACleanup();
        return -1;
    }

    // 绑定到本地地址和端口
    sockaddr_in localAddr;
    localAddr.sin_family = AF_INET;
    localAddr.sin_port = htons(PORT);
    localAddr.sin_addr.s_addr = INADDR_ANY;

    if (bind(receivingSocket, (sockaddr*)&localAddr, sizeof(localAddr)) == SOCKET_ERROR) {
        std::cerr << "Failed to bind socket: " << WSAGetLastError() << std::endl;
        closesocket(receivingSocket);
        WSACleanup();
        return -1;
    }

    // 等待握手
    sockaddr_in senderAddr;
    int senderAddrLen = sizeof(senderAddr);
    char handshakeBuffer[BUFFER_SIZE];
    int result = recvfrom(receivingSocket, handshakeBuffer, BUFFER_SIZE, 0, (sockaddr*)&senderAddr, &senderAddrLen);
    if (result == SOCKET_ERROR) {
        std::cerr << "Failed to receive handshake: " << WSAGetLastError() << std::endl;
        closesocket(receivingSocket);
        WSACleanup();
        return -1;
    }

    // 接收数据
    char buffer[BUFFER_SIZE];
    int totalReceived = 0;
    auto start = std::chrono::high_resolution_clock::now();  // 开始时间

    while (totalReceived < FILE_SIZE) {
        result = recvfrom(receivingSocket, buffer, BUFFER_SIZE, 0, (sockaddr*)&senderAddr, &senderAddrLen);
        if (result == SOCKET_ERROR) {
            std::cerr << "Failed to receive data: " << WSAGetLastError() << std::endl;
            break;
        }
        totalReceived += result;
    }

    auto end = std::chrono::high_resolution_clock::now();  // 结束时间
    double elapsedTime = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count() / 1000.0;  // 传输时间（秒）
    double transferSpeed = FILE_SIZE / (elapsedTime * 1024 * 1024);  // 传输速度（MB/s）
    std::cout << "Transfer speed: " << transferSpeed << " MB/s" << std::endl;

    // 关闭套接字
    closesocket(receivingSocket);
    WSACleanup();

    return 0;
}
