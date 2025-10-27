# CS407 Final Project Server

## Introduction

This is the backend server for the CS407 Final Project.  
It includes both the **Spring Boot REST API** (for login, user management, etc.) and the **Netty WebSocket Server** (for real-time communication).  

For detailed project information, please refer to our [Project Proposal](./CS407_Project_Proposal.pdf).

---

## Development Environment Setup

### Prerequisites
- Docker and Docker Compose
- Java 17
- Maven
- [Apidog](https://apidog.com/) for API testing
- [wscat](https://github.com/websockets/wscat) for WebSocket connection testing
- [Another Redis Desktop Manager](https://goanother.com/cn/) Redis client tool
- [dbeaver](https://dbeaver.io/) database management tool

### Getting Started

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd knot_server
   ```

2. **Start the development environment**
   ```bash
   # Start MySQL and Redis services in the background
   docker-compose up -d
   
   # Wait for services to be ready (especially on first MySQL startup)
   docker-compose ps
   ```

3. **Start the Spring Boot application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify everything is working**
   - Spring Boot Application: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html
   - GUI API Testing: http://localhost:8080/client/login.html
   - Netty WebSocket Server: ws://localhost:10827/ws

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
- **Backup User**: app_user (also no password)  

You can connect using any MySQL client:
- **Host**: localhost  
- **Port**: 3306  
- **Database**: cs407_final_project  
- **Username**: root  
- **Password**: (leave empty)  

---

## WebSocket Testing 

We use **JWT access token (AT)** obtained from the login API to authenticate WebSocket connections. You can test temporarily through the http://localhost:8080/client/login.html page.

### Preparation

1. **Install wscat tool**
   ```bash
   npm install -g wscat
   ```

2. **Login via REST API** to get the **access token (AT)**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"123456"}'
   ```
   Response example:
   ```json
   {
     "access_token": "<your_jwt_here>"
   }
   ```

### Connection and Authentication

1. **Connect to WebSocket server**:
   ```bash
   wscat -c ws://localhost:10827/ws
   ```

2. **Send AUTH message** (replace `<jwt>` with real JWT):
   ```json
   {"type":"AUTH","token":"<jwt>"}
   ```
   Expected response:
   ```json
   {"type":"AUTH_OK","uid":1}
   ```
   
   At this point, the following will be created in Redis:
   - `channel:{uid}` → maps user to channelId
   - `online:{uid}` → online marker (90-second TTL)

### Message Sending and Receiving

1. **Send message** (Client A):
   ```json
   {
     "type": "MSG_SEND",
     "convId": 1,
     "clientMsgId": "c-1696500000000",
     "msgType": 0,
     "contentText": "hello zsw!"
   }
   ```

2. **Message acknowledgment** (sender Client A will receive):
   ```json
   {
     "type": "MSG_ACK",
     "msgId": 7,
     "clientMsgId": "c-1696500000011",
     "serverTime": 1759694035205
   }
   ```

3. **New message notification** (receiver Client B will receive):
   ```json
   {
     "type": "MSG_NEW",
     "convId": 1,
     "fromUid": 4,
     "msgId": 101,
     "contentText": "hello zsw!"
   }
   ```

### Session Maintenance

1. **Send heartbeat message**:
   ```json
   {"type":"HEARTBEAT"}
   ```
   Expected response:
   ```json
   {
     "type": "HEARTBEAT_ACK",
     "uid": 1,
     "remain": 79,         // Remaining seconds before refresh
     "ttl": 90,            // Fixed TTL
     "serverTs": 1738530000000  // Server millisecond timestamp
   }
   ```

2. **Logout**:
   ```json
   {"type":"LOGOUT"}
   ```
   Expected response:
   ```json
   {"type":"LOGOUT_OK"}
   ```
   
   At this point:
   - `channel:{uid}` in Redis is removed (compare-and-del)
   - `online:{uid}` in Redis is deleted
   - Channel is closed

---

## Stopping the Environment

```bash
# Stop all services
docker-compose down

# Stop and remove data volumes (This will delete all data!)
docker-compose down -v
```

---

## Database Schema

The database schema is managed by **Flyway migration scripts** located in `src/main/resources/db/migration/`.  
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
│   │   ├── java/com/example/knot_server/                # Java source code
│   │   │   ├── config/                                  # Application configuration classes
│   │   │   │   ├── AppBeans.java                        # Bean configuration
│   │   │   │   ├── RedisScriptConfig.java               # Redis script configuration
│   │   │   │   ├── SecurityConfig.java                  # Security configuration
│   │   │   │   └── SwaggerConfig.java                   # Swagger API documentation configuration
│   │   │   ├── controller/                              # Controller layer
│   │   │   │   ├── AuthController.java                  # Authentication controller
│   │   │   │   ├── ConversationController.java          # Conversation controller
│   │   │   │   └── test/                                # Test controllers
│   │   │   ├── entity/                                  # Entity classes
│   │   │   │   ├── Conversation.java                    # Conversation entity
│   │   │   │   ├── ConversationMember.java              # Conversation member entity
│   │   │   │   ├── Message.java                         # Message entity
│   │   │   │   ├── MessageAttachment.java               # Message attachment entity
│   │   │   │   ├── MessageReceipt.java                  # Message receipt entity
│   │   │   │   ├── SingleConvIndex.java                 # Single conversation index entity
│   │   │   │   └── User.java                            # User entity
│   │   │   ├── mapper/                                  # MyBatis mappers
│   │   │   │   ├── ConversationMapper.java              # Conversation mapper
│   │   │   │   ├── ConversationMemberMapper.java        # Conversation member mapper
│   │   │   │   ├── MessageMapper.java                   # Message mapper
│   │   │   │   ├── MessageAttachmentMapper.java         # Message attachment mapper
│   │   │   │   ├── MessageReceiptMapper.java            # Message receipt mapper
│   │   │   │   ├── SingleConvIndexMapper.java           # Single conversation index mapper
│   │   │   │   └── UserMapper.java                      # User mapper
│   │   │   ├── netty/                                   # Netty WebSocket server
│   │   │   │   ├── server/                              # Server core components
│   │   │   │   │   ├── NettyServer.java                 # Netty server
│   │   │   │   │   ├── NettyServerInitializer.java      # Server initializer
│   │   │   │   │   ├── model/                           # Server models
│   │   │   │   │   │   └── WsSendMessage.java           # WebSocket send message
│   │   │   │   │   └── handler/                         # Message handlers
│   │   │   │   │       ├── AuthHandler.java             # Authentication handler
│   │   │   │   │       ├── CleanupHandler.java          # Cleanup handler
│   │   │   │   │       ├── HeartBeatHandler.java        # Heartbeat handler
│   │   │   │   │       ├── LogoutHandler.java           # Logout handler
│   │   │   │   │       └── MessageHandler.java          # Message handler
│   │   │   │   └── session/                             # Session management
│   │   │   │       ├── ChannelAttrs.java                # Channel attributes
│   │   │   │       ├── LocalSessionRegistry.java        # Local session registry (will be modified later for deployment)
│   │   │   │       └── SessionKeys.java                 # Session keys
│   │   │   ├── service/                                 # Service layer
│   │   │   │   ├── AuthService.java                     # Authentication service interface
│   │   │   │   ├── ConversationService.java             # Conversation service interface
│   │   │   │   ├── MessageService.java                  # Message service interface
│   │   │   │   ├── dto/                                 # Data transfer objects
│   │   │   │   │   └── MessageSavedView.java            # Message saved view
│   │   │   │   └── impl/                                # Service implementation classes
│   │   │   │       ├── AuthServiceImpl.java             # Authentication service implementation
│   │   │   │       ├── ConversationServiceImpl.java     # Conversation service implementation
│   │   │   │       └── MessageServiceImpl.java          # Message service implementation
│   │   │   ├── util/                                    # Utility classes
│   │   │   │   ├── JwtAuthFilter.java                   # JWT authentication filter
│   │   │   │   └── JwtService.java                      # JWT service
│   │   │   └── KnotServerApplication.java               # Application entry point
│   │   └── resources/
│   │       ├── application.yml                          # Application configuration file
│   │       ├── db/migration/                            # Flyway database migration scripts
│   │       │   ├── V1__init_schema.sql                  # Initialize schema
│   │       │   └── V2__messaging_schema.sql             # Messaging schema
│   │       └── static/                                  # Static resources
│   │           └── client/                              # Client test pages
│   │              └── login.html                        # Login test page
│   └── test/                                            # Test code
│       └── java/com/example/knot_server/                # Test source code
│           └── KnotServerApplicationTests.java          # Application test class
├── docker/
│   └── mysql/init/                                      # MySQL initialization scripts
│       └── 01-init.sql                                  # Database initialization SQL
├── .mvn/                                                # Maven configuration
├── target/                                              # Compilation output directory
├── docker-compose.yml                                   # Docker service configuration
├── pom.xml                                              # Maven project configuration
├── mvnw                                                 # Maven wrapper (Unix)
└── mvnw.cmd                                             # Maven wrapper (Windows)
```
│       └── 01-init.sql                                  # 数据库初始化SQL
├── .mvn/                                                # Maven配置
├── target/                                              # 编译输出目录
├── docker-compose.yml                                   # Docker服务配置
├── pom.xml                                              # Maven项目配置
├── mvnw                                                 # Maven包装器(Unix)
└── mvnw.cmd                                             # Maven包装器(Windows)
```
