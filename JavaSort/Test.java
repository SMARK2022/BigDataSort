
import java.util.ArrayList;
import java.util.List;

class Main {

    private static List<Long> thread_data_str=new ArrayList<>();

    public static void main(String[] args) {

        long a = 1024;
        for (long i = 0; i < 50331648 * 16; i++)
            thread_data_str.add(a);
        System.out.println("OK");
        while (true) {
        }
    }
}
