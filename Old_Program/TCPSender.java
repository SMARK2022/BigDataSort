import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPSender {
    public static void main(String[] args) {
        String serverIP = "127.0.0.1";
        int serverPort = 12345;
        int dataSize = 1024; // 每次发送的数据大小（单位为字节）        
        int epochs = 1000000; // 每次发送的数据大小（单位为字节）

        try {
            Socket socket = new Socket(serverIP, serverPort);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            // 进行握手
            outputStream.writeUTF("握手消息");

            // 开始计时测试
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < epochs; i++) {
                byte[] data = new byte[dataSize];
                outputStream.write(data);
            }
            long endTime = System.currentTimeMillis();

            // 输出测试结果
            long elapsedTime = endTime - startTime;
            double speed = (double) dataSize*epochs / elapsedTime / 1024 / 1024;
            System.out.println("发送端：数据发送完毕，总耗时：" + elapsedTime + "ms");
            System.out.println("发送速度：" + speed + "MB/s");

            outputStream.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
