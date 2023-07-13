#include <iostream>
#include <Winsock2.h>

#pragma comment(lib, "ws2_32.lib")

#define SERVER_IP "127.0.0.1" // 本机IP地址
#define SERVER_PORT 8888      // 接收端端口号

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
    SOCKET serverSocket = socket(AF_INET, SOCK_DGRAM, 0);
    if (serverSocket == INVALID_SOCKET)
    {
        std::cout << "Failed to create socket" << std::endl;
        WSACleanup();
        return -1;
    }

    // 设置本机地址信息
    sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = inet_addr(SERVER_IP);

    // 绑定socket到本机地址
    if (bind(serverSocket, (sockaddr *)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR)
    {
        std::cout << "Failed to bind socket" << std::endl;
        closesocket(serverSocket);
        WSACleanup();
        return -1;
    }

    // 接收握手包
    char recvBuffer[1024];
    memset(recvBuffer, 0, sizeof(recvBuffer));
    int recvLen = recvfrom(serverSocket, recvBuffer, sizeof(recvBuffer), 0, NULL, NULL);

    if (recvLen > 0)
    {
        std::cout << "Handshake received: " << recvBuffer << std::endl;
    }
    else
    {
        std::cout << "Failed to receive handshake" << std::endl;
        closesocket(serverSocket);
        WSACleanup();
        return -1;
    }

    // 接收数据并比对内容是否相同
    std::string expectedMsg = "Data";
    int numPacketsReceived = 0;
    int num = 10000000;
    for (int i = 0; i < num; i++)
    {
        memset(recvBuffer, 0, sizeof(recvBuffer));
        int recvLen = recvfrom(serverSocket, recvBuffer, sizeof(recvBuffer), 0, NULL, NULL);
        std::string receivedMsg = recvBuffer;
        // std::cout << "Received: " << receivedMsg << std::endl;
        if (receivedMsg == expectedMsg)
        {
            numPacketsReceived++;
        }
    }

    // 输出正确率
    float errorRate = 1.0f - ((float)numPacketsReceived / num);
    std::cout << "Error rate: " << errorRate << std::endl;

    // 关闭socket并清理Winsock
    closesocket(serverSocket);
    WSACleanup();

    return 0;
}
