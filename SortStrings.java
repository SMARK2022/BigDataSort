import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortStrings {

    public static void main(String[] args) {
        String fileName = "test.txt";
        List<String> strings = readStringsFromFile(fileName);
        long startTime = System.currentTimeMillis();
        sortStrings(strings);
        long endTime = System.currentTimeMillis();
        // printSortedStrings(strings);
        System.out.println("排序耗时: " + (endTime - startTime) + " 毫秒");
    }

    public static List<String> readStringsFromFile(String fileName) {
        List<String> strings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                strings.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strings;
    }

    public static void sortStrings(List<String> strings) {
        Collections.sort(strings);
    }

    public static void printSortedStrings(List<String> strings) {
        for (String str : strings) {
            System.out.println(str);
        }
    }
}
