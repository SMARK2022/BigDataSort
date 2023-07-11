import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPReceiver {
    public static void main(String[] args) {
        int listenPort = 12345;
        int totalSize = 0; // 总数据量（单位为字节）

        try {
            ServerSocket serverSocket = new ServerSocket(listenPort);
            System.out.println("接收端：等待连接...");

            Socket socket = serverSocket.accept();
            System.out.println("接收端：连接成功！");

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            // 进行握手
            String handshakeMsg = inputStream.readUTF();
            System.out.println("接收端：握手消息：" + handshakeMsg);

            // 开始计时测试
            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[1024]; // 接收缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                // 可以在这里对接收到的数据进行处理
                totalSize += bytesRead;
            }
            long endTime = System.currentTimeMillis();

            // 输出测试结果
            long elapsedTime = endTime - startTime;
            double speed = (double) totalSize / elapsedTime / 1024 / 1024;
            System.out.println("接收端：数据接收完毕，总耗时：" + elapsedTime + "ms");
            System.out.println("接收速度：" + speed + "MB/s");

            inputStream.close();
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
