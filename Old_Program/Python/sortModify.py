import os
import tempfile
import string


# 定义文件块大小为1GB
BLOCK_SIZE = 1024 * 1024 * 1024


def read_lines(start, rows, data, num):
    file_read = open(filename_READ, 'r')
    if file_read.closed:
        print("无法打开文件")
        return
    data = [[] for _ in range(NUM_STR_HIGH)]
    num = 0
    file_read.seek(start * 16, 0)
    while num < rows:
        line = file_read.readline().strip()
        if not line:
            break
        tmp_LowStr = sum(string.ascii_lowercase.index(c)  * 26 ** (13 - i) for i, c in enumerate(line[2:15]))
        data[(ord(line[0]) - ord('a')) * 26 + ord(line[1]) - ord('a')].append(tmp_LowStr)
        num += 1
    file_read.close()

def sort_file_chunks(input_file, output_file):
    # 创建临时目录用于存放排序后的小文件块
    temp_dir = tempfile.mkdtemp()
    
    # 切割大文件为较小的块，并对每个块进行快速排序
    with open(input_file, 'rb') as f:
        chunk_num = 0
        while True:
            chunk_data = f.read(BLOCK_SIZE)
            if not chunk_data:
                break
            
            # 将数据写入临时文件
            temp_file = os.path.join(temp_dir, f'chunk_{chunk_num}.dat')
            with open(temp_file, 'wb') as temp_f:
                temp_f.write(chunk_data)
            
            # 快速排序临时文件中的数据
            with open(temp_file, 'rb+') as temp_f:
                data = list(temp_f)
                data.sort()
                temp_f.seek(0)
                temp_f.writelines(data)
            
            chunk_num += 1
    
    # 归并排序合并排序好的小文件块
    output_data = []
    chunk_files = sorted(os.listdir(temp_dir))
    for chunk_file in chunk_files:
        chunk_file = os.path.join(temp_dir, chunk_file)
        with open(chunk_file, 'rb') as temp_f:
            output_data.extend(temp_f)
    
    # 将排序后的数据写入最终输出文件
    with open(output_file, 'wb') as output_f:
        output_f.writelines(output_data)
    
    # 删除临时文件和目录
    for chunk_file in chunk_files:
        chunk_file = os.path.join(temp_dir, chunk_file)
        os.remove(chunk_file)
    os.rmdir(temp_dir)

# 示例用法
input_file = 'data01.txt'
output_file = 'results.txt'
sort_file_chunks(input_file, output_file)
print(f'排序后的数据已写入文件：{output_file}')