# BigDataSort

Nine freshmen's project which can sort 120G data of strings.
欢迎大家自己实验，可以自己创建一个新文件，用来存放自己代码做实验，也欢迎修改代码进行补充（记得做好备份）

目前的文件组成：

- `generate.cpp`：用来生成随机字符串
- `SortStr.cpp`：基于 STL vector<Str> 自定义数据类型 容器实现的 stable_sort 与 tim_sort，速度大概单线程 `160M/5s`
- `SortString.cpp`：基于 STL vector<std::string> 容器实现的 stable_sort 与 tim_sort，速度大概单线程 `160M/10s`
- `SortString.java`: 基于 Java String 列表调用官方默认的 TimSort 方法，速度大概多线程 `160M/5s`

我们可以得出一些经验：

- 使用自己实现的结构体可以加快排序的操作速度，（应该是减小了内存的复制量与内存地址不对齐的问题）

我们未来可以实现的功能，欢迎大家积极尝试（可以直接 GPT 生成代码自己调试）:

- `[ ]`实现多线程操作，从而完整调用多核性能，并可以想一下归并的思路
- `[ ]`将 STL 的 vector 容器改为直接的结构体数组操作，这样可以加快排序迭代与内存分配的效率
