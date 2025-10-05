# CS407 Final Project Server

## Introduction

This is the backend server for the CS407 Final Project.  
It includes both the **Spring Boot REST API** (for login, user management, etc.) and the **Netty WebSocket Server** (for real-time communication).  

For detailed project information, please refer to our [Project Proposal](./CS407_Project_Proposal.pdf).

---

## Development Setup

### Prerequisites
- Docker and Docker Compose
- Java 17
- Maven
- [Apidog](https://apidog.com/) for testing api
- [wscat](https://github.com/websockets/wscat) for testing WebSocket connections
- [Another Redis Desktop Manager](https://goanother.com/cn/) redis
- [dbeaver](https://dbeaver.io/) database manager

### Getting Started

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd knot_server
   ```

2. **Start the development environment**
   ```bash
   # Start MySQL and Redis services in background
   docker-compose up -d
   
   # Wait for services to be ready (especially MySQL on first startup)
   docker-compose ps
   ```

3. **Start the Spring Boot application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify everything is working**
   - Spring Boot app: http://localhost:8080
   - API documentation: http://localhost:8080/swagger-ui.html
   - GUI API testing: http://localhost:8080/client/login.html
   - Netty WebSocket server: ws://localhost:10827/ws

---

## Service Ports

- **Spring Boot Application**: http://localhost:8080  
- **Netty WebSocket**: ws://localhost:10827/ws  
- **MySQL Database**: localhost:3306  
- **Redis Cache**: localhost:6379  

---

## Database Information

- **Database Name**: cs407_final_project  
- **Username**: root  
- **Password**: (empty)  
- **Alternative User**: app_user (also no password)  

You can use any MySQL client to connect:
- **Host**: localhost  
- **Port**: 3306  
- **Database**: cs407_final_project  
- **Username**: root  
- **Password**: (leave empty)  

---

## WebSocket Testing 

我们使用 **JWT access token (AT)** 从登录 API 获取来认证 WebSocket 连接。您可以通过 http://localhost:8080/client/login.html 页面进行临时测试。

### 准备工作

1. **安装 wscat 工具**
   ```bash
   npm install -g wscat
   ```

2. **通过 REST API 登录** 获取 **access token (AT)**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"123456"}'
   ```
   响应示例:
   ```json
   {
     "access_token": "<your_jwt_here>"
   }
   ```

### 连接与认证

1. **连接到 WebSocket 服务器**:
   ```bash
   wscat -c ws://localhost:10827/ws
   ```

2. **发送 AUTH 消息** (用真实的 JWT 替换 `<jwt>`):
   ```json
   {"type":"AUTH","token":"<jwt>"}
   ```
   预期响应:
   ```json
   {"type":"AUTH_OK","uid":1}
   ```
   
   此时 Redis 中会创建:
   - `channel:{uid}` → 映射用户到 channelId
   - `online:{uid}` → 在线标记 (90秒 TTL)

### 消息收发

1. **发送消息** (Client A):
   ```json
   {
     "type": "MSG_SEND",
     "convId": 1,
     "clientMsgId": "c-1696500000000",
     "msgType": 0,
     "contentText": "hello zsw!"
   }
   ```

2. **消息回执** (发送方 Client A 会收到):
   ```json
   {
     "type": "MSG_ACK",
     "msgId": 7,
     "clientMsgId": "c-1696500000011",
     "serverTime": 1759694035205
   }
   ```

3. **新消息通知** (接收方 Client B 会收到):
   ```json
   {
     "type": "MSG_NEW",
     "convId": 1,
     "fromUid": 4,
     "msgId": 101,
     "contentText": "hello zsw!"
   }
   ```

### 会话维护

1. **发送心跳消息**:
   ```json
   {"type":"HEARTBEAT"}
   ```
   预期响应:
   ```json
   {
     "type": "HEARTBEAT_ACK",
     "uid": 1,
     "remain": 79,         // 刷新前剩余秒数
     "ttl": 90,            // 固定 TTL
     "serverTs": 1738530000000  // 服务端毫秒时间戳
   }
   ```

2. **退出登录**:
   ```json
   {"type":"LOGOUT"}
   ```
   预期响应:
   ```json
   {"type":"LOGOUT_OK"}
   ```
   
   此时:
   - Redis 中的 `channel:{uid}` 被移除 (compare-and-del)
   - Redis 中的 `online:{uid}` 被删除
   - 通道关闭

---

## Stopping the Environment

```bash
# Stop all services
docker-compose down

# Stop and remove data volumes (this will delete all data!)
docker-compose down -v
```

---

## Database Schema

The database schema is managed by **Flyway migrations** located in `src/main/resources/db/migration/`.  
When the application starts, it will automatically run any pending migrations.

---

## Troubleshooting

If you encounter database connection issues:

1. Make sure Docker is running  
2. Check if port 3306 is available  
3. Wait for MySQL to fully start (first startup may take a few minutes)  

```bash
# Check MySQL container logs
docker-compose logs mysql

# Restart MySQL service
docker-compose restart mysql
```

---

## Current Project Structure

```
knot_server/
├── src/
│   ├── main/
│   │   ├── java/com/example/knot_server/    # Java source code
│   │   │   ├── config/                      # 应用配置类
│   │   │   │   ├── AppBeans.java            # Bean 配置
│   │   │   │   ├── RedisScriptConfig.java   # Redis 脚本配置
│   │   │   │   ├── SecurityConfig.java      # 安全配置
│   │   │   │   └── SwaggerConfig.java       # Swagger API 文档配置
│   │   │   ├── controller/                  # 控制器层
│   │   │   │   ├── AuthController.java      # 认证相关控制器
│   │   │   │   ├── ConversationController.java # 会话控制器
│   │   │   │   └── test/                    # 测试控制器
│   │   │   ├── entity/                      # 实体类
│   │   │   │   ├── Conversation.java        # 会话实体
│   │   │   │   ├── ConversationMember.java  # 会话成员实体
│   │   │   │   ├── Message.java             # 消息实体
│   │   │   │   ├── MessageAttachment.java   # 消息附件实体
│   │   │   │   ├── MessageReceipt.java      # 消息回执实体
│   │   │   │   ├── SingleConvIndex.java     # 单聊索引实体
│   │   │   │   └── User.java                # 用户实体
│   │   │   ├── mapper/                      # MyBatis映射器
│   │   │   │   ├── ConversationMapper.java  # 会话映射器
│   │   │   │   ├── ConversationMemberMapper.java # 会话成员映射器
│   │   │   │   ├── MessageMapper.java       # 消息映射器
│   │   │   │   ├── MessageAttachmentMapper.java # 消息附件映射器
│   │   │   │   ├── MessageReceiptMapper.java # 消息回执映射器
│   │   │   │   ├── SingleConvIndexMapper.java # 单聊索引映射器
│   │   │   │   └── UserMapper.java          # 用户映射器
│   │   │   ├── netty/                       # Netty WebSocket服务器
│   │   │   │   ├── server/                  # 服务器核心组件
│   │   │   │   │   ├── NettyServer.java     # Netty 服务器
│   │   │   │   │   ├── NettyServerInitializer.java # 服务器初始化
│   │   │   │   │   ├── model/               # 服务器模型
│   │   │   │   │   │   └── WsSendMessage.java # WebSocket 发送消息
│   │   │   │   │   └── handler/             # 消息处理器
│   │   │   │   │       ├── AuthHandler.java  # 认证处理
│   │   │   │   │       ├── CleanupHandler.java # 清理处理
│   │   │   │   │       ├── HeartBeatHandler.java # 心跳处理
│   │   │   │   │       ├── LogoutHandler.java # 登出处理
│   │   │   │   │       └── MessageHandler.java # 消息处理
│   │   │   │   └── session/                 # 会话管理
│   │   │   │       ├── ChannelAttrs.java    # 通道属性
│   │   │   │       ├── LocalSessionRegistry.java # 本地会话注册 (之后部署会修改)
│   │   │   │       └── SessionKeys.java     # 会话键
│   │   │   ├── service/                     # 服务层
│   │   │   │   ├── AuthService.java         # 认证服务接口
│   │   │   │   ├── ConversationService.java # 会话服务接口
│   │   │   │   ├── MessageService.java      # 消息服务接口
│   │   │   │   ├── dto/                     # 数据传输对象
│   │   │   │   │   └── MessageSavedView.java # 消息保存视图
│   │   │   │   └── impl/                    # 服务实现类
│   │   │   │       ├── AuthServiceImpl.java # 认证服务实现
│   │   │   │       ├── ConversationServiceImpl.java # 会话服务实现
│   │   │   │       └── MessageServiceImpl.java # 消息服务实现
│   │   │   ├── util/                        # 工具类
│   │   │   │   ├── JwtAuthFilter.java       # JWT认证过滤器
│   │   │   │   └── JwtService.java          # JWT服务
│   │   │   └── KnotServerApplication.java   # 应用入口类
│   │   └── resources/
│   │       ├── application.yml              # 应用配置文件
│   │       ├── db/migration/                # Flyway数据库迁移脚本
│   │       │   ├── V1__init_schema.sql      # 初始化架构
│   │       │   └── V2__messaging_schema.sql # 消息架构
│   │       └── static/                      # 静态资源
│   │           └── client/                  # 客户端测试页面
│   │              └── login.html           # 登录测试页面
│   └── test/                                # 测试代码
│       └── java/com/example/knot_server/    # 测试源代码
│           └── KnotServerApplicationTests.java # 应用测试类
├── docker/
│   └── mysql/init/                          # MySQL初始化脚本
│       └── 01-init.sql                      # 数据库初始化SQL
├── .mvn/                                    # Maven配置
├── target/                                  # 编译输出目录
├── docker-compose.yml                       # Docker服务配置
├── pom.xml                                  # Maven项目配置
├── mvnw                                     # Maven包装器(Unix)
└── mvnw.cmd                                 # Maven包装器(Windows)
```
