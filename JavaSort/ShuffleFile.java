import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class ShuffleFile {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入文件路径：");
        String filePath = scanner.nextLine();

        List<String> lines = readLinesFromFile(filePath);
        if (lines.isEmpty()) {
            System.out.println("文件为空或读取文件失败。");
            return;
        }

        List<String> shuffledLines = shuffleLines(lines);

        String outputFilePath = "shuffled_" + filePath;
        if (writeLinesToFile(outputFilePath, shuffledLines)) {
            System.out.println("文件已生成：" + outputFilePath);
        } else {
            System.out.println("生成文件失败。");
        }
    }

    private static List<String> readLinesFromFile(String filePath) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    private static List<String> shuffleLines(List<String> lines) {
        List<String> shuffledLines = new ArrayList<>(lines);
        Collections.shuffle(shuffledLines, new Random());
        return shuffledLines;
    }

    private static boolean writeLinesToFile(String filePath, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
