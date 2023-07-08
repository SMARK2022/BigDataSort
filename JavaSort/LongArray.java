import java.util.Arrays;

public class LongArray {
    private long[] arr;
    private int size;

    public LongArray() {
        arr = new long[10];
        size = 0;
    }

    public void push(long value) {
        if (size == arr.length) {
            // 扩充数组大小
            arr = Arrays.copyOf(arr, arr.length * 2);
        }
        arr[size++] = value;
    }

    public long get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return arr[index];
    }

    public void set(int index, long value) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        arr[index] = value;
    }

    public void sort() {
        Arrays.sort(arr, 0, size);
    }
}
