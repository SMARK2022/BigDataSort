import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CompareFiles {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入第一个文件路径：");
        String filePath1 = scanner.nextLine();

        System.out.print("请输入第二个文件路径：");
        String filePath2 = scanner.nextLine();

        List<String> lines1 = readLinesFromFile(filePath1);
        List<String> lines2 = readLinesFromFile(filePath2);

        if (lines1.isEmpty() || lines2.isEmpty()) {
            System.out.println("文件为空或读取文件失败。");
            return;
        }

        boolean areFilesEqual = compareFiles(lines1, lines2);
        if (areFilesEqual) {
            System.out.println("这两个文件完全一样。");
        } else {
            System.out.println("这两个文件不完全一样。");
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

    private static boolean compareFiles(List<String> lines1, List<String> lines2) {
        if (lines1.size() != lines2.size()) {
            return false;
        }

        for (int i = 0; i < lines1.size(); i++) {
            String line1 = lines1.get(i);
            String line2 = lines2.get(i);

            if (!line1.equals(line2)) {
                return false;
            }
        }

        return true;
    }
}
