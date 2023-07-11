import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UDPExample {

    public static void main(String[] args) {
        // 发送方
        Thread senderThread = new Thread(() -> {
            try {
                // 创建UDP Socket
                DatagramSocket socket = new DatagramSocket();

                // 准备发送的数据
                LongArray longArray = new LongArray();
                longArray.push(1);
                longArray.push(2);
                longArray.push(3);
                byte[] sendData = longArray.ExporttoBytes();

                // 发送前的信息
                int blockSize = sendData.length; // 数据块的大小
                int bucketNumber = 1; // 归并段编号
                int hostNumber = 2; // 主机编号
                int number = 3; // 编号

                // 将发送前的信息合并成一个字节数组
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream dataStream = new DataOutputStream(byteStream);
                dataStream.writeInt(blockSize);
                dataStream.writeInt(bucketNumber);
                dataStream.writeInt(hostNumber);
                dataStream.writeInt(number);
                dataStream.write(sendData);
                byte[] sendBytes = byteStream.toByteArray();

                // 创建一个DatagramPacket对象，指定发送的数据、目标地址和端口
                InetAddress address = InetAddress.getByName("localhost");
                int port = 12345;
                DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length, address, port);

                // 发送数据
                socket.send(packet);

                // 关闭Socket
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 接收方
        Thread receiverThread = new Thread(() -> {
            try {
                // 创建UDP Socket，指定端口号
                int port = 12345;
                DatagramSocket socket = new DatagramSocket(port);

                // 创建一个字节数组用于接收数据
                byte[] receiveData = new byte[1024];

                // 创建一个DatagramPacket对象，用于接收数据
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

                // 接收数据
                socket.receive(packet);

                // 获取接收到的字节数组
                byte[] receiveBytes = packet.getData();

                // 读取发送前的信息
                DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(receiveBytes));
                int blockSize = dataStream.readInt();
                int bucketNumber = dataStream.readInt();
                int hostNumber = dataStream.readInt();
                int number = dataStream.readInt();

                byte[] longArrayBytes = new byte[blockSize];
                System.arraycopy(receiveBytes, 16, longArrayBytes, 0, blockSize);

                LongArray longArray = LongArray.LoadfromBytes(longArrayBytes);
                System.out.println("接收到的LongArray：" + longArray);
                for (int i = 0; i < longArray.size(); i++) {
                    System.out.print(i);
                    System.out.print(" ");
                    System.out.println(longArray.get(i));

                }

                // 关闭Socket
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 启动发送方和接收方线程
        senderThread.start();
        receiverThread.start();
    }
}
