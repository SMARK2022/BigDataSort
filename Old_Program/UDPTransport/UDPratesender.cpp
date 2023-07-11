#include <iostream>
#include <Winsock2.h>

#pragma comment(lib, "ws2_32.lib")

#define SERVER_IP "127.0.0.1" // 接收端IP地址
#define SERVER_PORT 8888 // 接收端端口号

int main()
{
    // 初始化Winsock
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0)
    {
        std::cout << "Failed to initialize winsock" << std::endl;
        return -1;
    }

    // 创建UDP socket
    SOCKET clientSocket = socket(AF_INET, SOCK_DGRAM, 0);
    if (clientSocket == INVALID_SOCKET)
    {
        std::cout << "Failed to create socket" << std::endl;
        WSACleanup();
        return -1;
    }

    // 设置接收端地址信息
    sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = inet_addr(SERVER_IP);

    // 发送握手包
    std::string handshakeMsg = "Handshake";
    sendto(clientSocket, handshakeMsg.c_str(), handshakeMsg.length(), 0, (sockaddr*)&serverAddr, sizeof(serverAddr));

    // 持续发送数据
    std::string dataMsg = "Data";
    int num=10000000;
    for (int i = 0; i < num; i++)
    {
        sendto(clientSocket, dataMsg.c_str(), dataMsg.length(), 0, (sockaddr*)&serverAddr, sizeof(serverAddr));
        // Sleep(1000); // 每隔1秒发送一次数据
    }

    // 关闭socket并清理Winsock
    closesocket(clientSocket);
    WSACleanup();

    return 0;
}
