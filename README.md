# NetworkTools

> 一个常用网络工具集

![image](https://img.shields.io/github/downloads/MX233/NetworkTools/total)

进入[Releases](https://github.com/MX233/NetworkTools/releases) 后点击`NetworkTools-x.x.x.mirai2.jar` 下载

__前置插件:__
- [`chat-command`](https://github.com/project-mirai/chat-command)

目前已实现
- > `Ping`/`TcpPing` 连接测试
- > `Tracert` 路由追踪
- > `Web` 网页基本信息查询
- > `Dns` 域名解析查询
- > `Nmap` 端口扫描
- > `DoH` Dns over Https 加密DNS查询
- > `gc` 查墙

修改配置文件后需使用 `/ntools reload` 重载配置生效
查看菜单帮助 `/ntools help`

功能介绍

`<>` 为必须

`[]` 为可选

| 功能            | 命令格式                               | 命令示例                                         | PS                                       |
|---------------|:-----------------------------------|:---------------------------------------------|:-----------------------------------------|
| Ping          | `/ping` `<目标名称>` `[超时时间]`          | `/ping github.com 3 5000`                    | 地址后加`:`为TcpPing 示例`/ping github.com:443` |
| Tracert       | `/tr` `<目标名称>`                     | `/tr github.com`                             | -                                        |
| Web           | `/web` `<URL链接>` `[编码]`            | `/web https://github.com`                    | -                                        |
| Dns           | `/dns` `<目标名称>` `[类型]` `[DNS]`     | `/dns github.com A`                          | -                                        |
| Nmap          | `/nmap` `<目标名称>` `[端口]`            | `/nmap github.com` `/nmap github.com 80,443` | 不指定 默认扫前1000个端口                          |
| Nmap          | `/nmap` `<目标名称>` `[起始端口]` `[结束端口]` | `/nmap github.com 1 2000`                    | 扫描从起始到结束的端口                              |
| DoH           | `/doh` `<目标名称>` `[类型]`             | `/doh google.com`                            | 默认使用的DoH服务器已被中国大陆封锁 请先在配置文件设置代理          |
| gc(GFW Check) | `/gc` `<目标名称>`                     | `/gc github.com`                             | 在线测试经常抽风 建议参考GFWList                     |

请查询[`Permission Command文档`](https://github.com/mamoe/mirai/blob/dev/mirai-console/docs/BuiltInCommands.md#permissioncommand) 配置权限或

### 快速入门
给您或他人添加权限
    
    /perm add u<您的qq号> top.cutestar.networktools:*

给某个群所有成员添加权限
    
    /perm add m<群号码>.* top.cutestar.networktools:*

给所有群添加权限

    /perm add m* top.cutestar.networktools:*
