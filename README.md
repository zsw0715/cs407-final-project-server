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

## WebSocket Testing (with wscat)

We use **JWT access token (AT)** obtained from the login API to authenticate WebSocket connections.

1. **Install wscat**
   ```bash
   npm install -g wscat
   ```

2. **Login via REST API** (using Postman or curl) to obtain an **access token (AT)**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login         -H "Content-Type: application/json"         -d '{"username":"admin","password":"123456"}'
   ```
   Response example:
   ```json
   {
     "access_token": "<your_jwt_here>"
   }
   ```

3. **Connect to the WebSocket server**:
   ```bash
   wscat -c ws://localhost:10827/ws
   ```

4. **Send AUTH message** (replace `<jwt>` with the real access token):
   ```json
   {"type":"AUTH","token":"<jwt>"}
   ```
   Expected response:
   ```json
   {"type":"AUTH_OK","uid":1}
   ```

5. **Check Redis keys**:
   - `channel:{uid}` → maps user to channelId
   - `online:{uid}` → presence marker with TTL (90s)

6. **Send LOGOUT message**:
   ```json
   {"type":"LOGOUT"}
   ```
   Expected response:
   ```json
   {"type":"LOGOUT_OK"}
   ```
   At this point:
   - Redis `channel:{uid}` is removed (compare-and-del).
   - Redis `online:{uid}` is deleted.
   - Channel is closed.

7. **Send HEARTBEAT message**:
   ```json
   {"type":"HEARTBEAT"}
   ```
      Expected response:
   ```json
   {
   "type": "HEARTBEAT_ACK",
   "uid": 1,
   "remain": 79,        // 刷新前剩余秒数
   "ttl": 90,           // 固定 TTL
   "serverTs": 1738530000000  // 服务端毫秒时间戳
   }
   ```

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
│   │   │   │   ├── AppBeans.java            # Bean配置
│   │   │   │   ├── RedisScriptConfig.java   # Redis脚本配置
│   │   │   │   ├── SecurityConfig.java      # 安全配置
│   │   │   │   └── SwaggerConfig.java       # Swagger API文档配置
│   │   │   ├── controller/                  # 控制器层
│   │   │   │   ├── auth/                    # 认证相关控制器
│   │   │   │   └── test/                    # 测试控制器
│   │   │   ├── entity/                      # 实体类
│   │   │   │   └── User.java                # 用户实体
│   │   │   ├── mapper/                      # MyBatis映射器
│   │   │   ├── netty/                       # Netty WebSocket服务器
│   │   │   │   ├── server/                  # 服务器核心组件
│   │   │   │   └── session/                 # 会话管理
│   │   │   ├── service/                     # 服务层
│   │   │   │   ├── AuthService.java         # 认证服务接口
│   │   │   │   └── impl/                    # 服务实现类
│   │   │   ├── util/                        # 工具类
│   │   │   └── KnotServerApplication.java   # 应用入口类
│   │   └── resources/
│   │       ├── application.yml              # 应用配置文件
│   │       ├── db/migration/                # Flyway数据库迁移脚本
│   │       ├── static/                      # 静态资源
│   │       ├── templates/                   # 模板文件
│   │       └── META-INF/                    # 元数据
│   └── test/                                # 测试代码
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
