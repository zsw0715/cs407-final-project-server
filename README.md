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
- (Optional) [wscat](https://github.com/websockets/wscat) for testing WebSocket connections

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
├── src/main/
│   ├── java/com/example/knot_server/    # Java source code
│   └── resources/
│       ├── application.yml              # Application configuration
│       └── db/migration/                # Flyway database migrations
├── docker/
│   └── mysql/init/                      # MySQL initialization scripts
├── docker-compose.yml                   # Docker services configuration
└── pom.xml                              # Maven dependencies
```
