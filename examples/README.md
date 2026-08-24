# Examples · 示例项目

本目录用于存放 **clone 即跑** 的演示项目，是社区理解 LieShouCloud 架构的最佳入口。

## 规划中的示例

| 示例 | 演示内容 | 状态 |
| --- | --- | --- |
| `core-minimal` | core 底座最小可运行演示（单体模式） | 规划中 |
| `iot-demo` | 设备数据采集 → 时序展示（需商业版 iot 能力域，仅演示数据流） | 规划中 |

## 如何运行 core-minimal（待补充）

```bash
# 单体模式：一个进程跑 gateway + auth + user + admin + approval
java -jar app-monolith/target/*.jar --spring.profiles.active=monolith

# 浏览器打开 http://localhost:8080 使用 admin 控制台
```

> 想贡献示例？欢迎提交 PR——用最少的代码展示一个可运行的架构切片。
