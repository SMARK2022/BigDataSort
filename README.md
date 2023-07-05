# BigDataSort

Nine freshmen's project which can sort 120G data of strings.

目前的文件组成：

- `generate.cpp`：用来生成随机字符串
- `sort.cpp`：基于 STL 容器实现的 stable_sort 与 tim_sort，速度大概单线程 `160M/5s`
- `SortStrings.java`: 基于 Java String 列表调用官方默认的 TimSort 方法，速度大概多线程 `160M/5s`
- `test.cpp`：用来测试
