import threading
import time
import os
import string
from bisect import insort_left

NUM_STR_HIGH = 676
INIT_LINES = 10000000
filename_READ = ""


def cmp_func(a, b):
    return a < b


def read_lines(start, rows, data):
    with open(filename_READ, "r") as file_read:
        file_read.seek(start * 16, os.SEEK_SET)
        for _ in range(NUM_STR_HIGH):
            data.append([])
        for _ in range(rows):
            line = file_read.readline().strip()
            if not line:
                break
            tmp_LowStr = sum(
                string.ascii_lowercase.index(c) * (26 ** (13 - i))
                for i, c in enumerate(line[2:15])
            )
            data[(ord(line[0]) - ord("a")) * 26 + ord(line[1]) - ord("a")].append(
                tmp_LowStr
            )
        file_read.close()


def sort_data(thread_id, data):
    sort_start = time.time()
    for i in range(NUM_STR_HIGH):
        data[i].sort()
    sort_end = time.time()
    sort_duration = int((sort_end - sort_start) * 1000)
    print(f"Thread{thread_id} 分段排序耗时: {sort_duration} 毫秒")


def merge(data1, data2):
    merged_data_collection = []
    merge_start = time.time()
    for high in range(NUM_STR_HIGH):
        dataA = data1[high]
        dataB = data2[high]
        merged_data = []
        i = 0
        j = 0
        while i < len(dataA) and j < len(dataB):
            if cmp_func(dataA[i], dataB[j]):
                merged_data.append(dataA[i])
                i += 1
            else:
                merged_data.append(dataB[j])
                j += 1
        while i < len(dataA):
            merged_data.append(dataA[i])
            i += 1
        while j < len(dataB):
            merged_data.append(dataB[j])
            j += 1
        merged_data_collection.append(merged_data)
    merge_end = time.time()
    merge_duration = int((merge_end - merge_start) * 1000)
    print(f"归并排序耗时: {merge_duration} 毫秒")
    return merged_data_collection


def multi_merge(thread_data_str):
    merge_start = time.time()
    lock = threading.Lock()
    while len(thread_data_str) > 1:
        new_temp_data = []
        merge_threads = []
        for i in range(0, len(thread_data_str), 2):
            if i + 1 < len(thread_data_str):
                print(i)
                merge_threads.append(
                    threading.Thread(
                        target=lambda: append_with_lock(
                            lock,
                            new_temp_data,
                            merge(thread_data_str[i], thread_data_str[i + 1]),
                        ),
                        daemon=True,
                    )
                )
            else:
                new_temp_data.append(thread_data_str[i])
        for thread in merge_threads:
            thread.start()
        for thread in merge_threads:
            thread.join()
        thread_data_str = new_temp_data
    merged_data = thread_data_str[0]
    merge_end = time.time()
    merge_duration = int((merge_end - merge_start) * 1000)
    print(f"归并完成耗时: {merge_duration} 毫秒")
    return merged_data


def append_with_lock(lock, data, item):
    lock.acquire()
    try:
        data.append(item)
    finally:
        lock.release()


def main():
    global filename_READ
    # if len(os.sys.argv) < 2:
    #     print("请提供文件名")
    #     return

    # filename_READ = os.sys.argv[1]
    filename_READ = "data01.txt"
    print(f"文件名: {filename_READ}")

    all_start = time.time()
    read_start = time.time()

    with open(filename_READ, "r") as file_read:
        fileSize = os.path.getsize(filename_READ)
        count = fileSize // 16
        print(f"文件大小为：{fileSize / 1024 / 1024 / 1024}GB")
        print(f"文件行数为：{count}")

    thread_count = os.cpu_count()
    lines_per_thread = count // thread_count

    thread_data_str = [[] for _ in range(thread_count)]

    read_threads = []
    for i in range(thread_count):
        start = i * lines_per_thread
        end = count if i == thread_count - 1 else lines_per_thread * (i + 1)
        read_threads.append(
            threading.Thread(
                target=read_lines,
                args=(start, end - start, thread_data_str[i]),
                daemon=True,
            )
        )

    for thread in read_threads:
        thread.start()

    for thread in read_threads:
        thread.join()

    read_duration = int((time.time() - read_start) * 1000)
    print(f"读取数据耗时: {read_duration} 毫秒")

    sort_threads = []
    for i in range(thread_count):
        sort_threads.append(
            threading.Thread(
                target=sort_data, args=(i, thread_data_str[i]), daemon=True
            )
        )

    for thread in sort_threads:
        thread.start()

    for thread in sort_threads:
        thread.join()

    merged_data = multi_merge(thread_data_str)

    all_duration = int((time.time() - all_start) * 1000)
    print(f"总共处理耗时: {all_duration} 毫秒")
    print(f"总共处理速度: {fileSize / 1024 / 1024 * 1000 / all_duration} MB/s")

    for i in range(NUM_STR_HIGH):
        if merged_data[i]:
            print(merged_data[i][0], end=" ")
    print()


if __name__ == "__main__":
    main()
