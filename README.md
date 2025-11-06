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
   - GUI API Testing: http://localhost:8080/client/debug.html
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

We use **JWT access token (AT)** obtained from the login API to authenticate WebSocket connections. You can test temporarily through the http://localhost:8080/client/debug.html page.

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

### Friend Management System

The friend management system uses a **pure WebSocket architecture**, ensuring all operations are performed through real-time connections for architectural consistency.

#### 1. Send Friend Request

**Client A (User 1) sends request to User 2**:
```json
{
  "type": "FRIEND_REQUEST_SEND",
  "receiverId": 2,
  "message": "Hi, I'm User1. Let's be friends!"
}
```

**Client A receives acknowledgment**:
```json
{
  "type": "FRIEND_REQUEST_ACK",
  "requestId": 1,
  "status": "sent",
  "timestamp": 1699999999000
}
```

**Client B (User 2) receives real-time push notification**:
```json
{
  "type": "FRIEND_REQUEST_NEW",
  "requestId": 1,
  "fromUser": {
    "userId": 1,
    "username": "user1",
    "avatarUrl": "https://..."
  },
  "message": "Hi, I'm User1. Let's be friends!",
  "timestamp": 1699999999000
}
```

#### 2. Accept Friend Request

**Client B (User 2) accepts the request**:
```json
{
  "type": "FRIEND_REQUEST_ACCEPT",
  "requestId": 1
}
```

**Client B Received confirmation response**：
```json
{
  "type": "FRIEND_REQUEST_ACK",
  "requestId": 1,
  "status": "accepted",
  "convId": 123,
  "timestamp": 1699999999000
}
```

**Client A (User 1) receives real-time push notification**:
```json
{
  "type": "FRIEND_ADDED",
  "requestId": 1,
  "friend": {
    "userId": 2,
    "username": "user2",
    "avatarUrl": "https://..."
  },
  "convId": 123,
  "timestamp": 1699999999000
}
```

After accepting the request, the system will:
- Create friend relationship (in `friends` table)
- Automatically create a single conversation (convId returned in response)
- Both users can start chatting immediately

#### 3. Reject Friend Request

**Client B (User 2) rejects the request**:
```json
{
  "type": "FRIEND_REQUEST_REJECT",
  "requestId": 1
}
```

**Client B receives acknowledgment**:
```json
{
  "type": "FRIEND_REQUEST_ACK",
  "requestId": 1,
  "status": "rejected",
  "timestamp": 1699999999000
}
```

#### 4. Resend Request

If a request is rejected, the requester can send another request:
- The system will automatically update the previous request record to pending status
- The receiver will receive a new `FRIEND_REQUEST_NEW` push notification
- Avoids database unique constraint conflicts

---

### Map Post System

The Map Post system allows users to create location-based social posts that can be shared with friends. Posts are created via **WebSocket long connection** and include geographic coordinates, media content, and visibility settings.

#### 1. Create Map Post (All Friends Visible)

**Client sends creation request**:
```json
{
  "type": "MAP_POST_CREATE",
  "clientReqId": "test-uuid-12345",
  "title": "Amazing Coffee Shop in Sanlitun",
  "description": "Just discovered this hidden gem! Great latte and cozy atmosphere ☕",
  "mediaUrls": [
    "https://s3.amazonaws.com/photos/cafe-interior.jpg",
    "https://s3.amazonaws.com/photos/latte.jpg"
  ],
  "loc": {
    "lat": 39.9342,
    "lng": 116.4478,
    "name": "Sanlitun SOHO"
  },
  "allFriends": true,
  "memberIds": []
}
```

**Creator receives acknowledgment**:
```json
{
  "type": "MAP_POST_ACK",
  "clientReqId": "test-uuid-12345",
  "mapPostId": 1,
  "convId": 1001,
  "serverTime": 1699268400000
}
```

**All friends receive real-time push notification**:
```json
{
  "type": "MAP_POST_NEW",
  "mapPostId": 1,
  "convId": 1001,
  "creatorId": 100,
  "creatorUsername": "alice",
  "title": "Amazing Coffee Shop in Sanlitun",
  "description": "Just discovered this hidden gem! Great latte and cozy atmosphere ☕",
  "mediaUrls": [
    "https://s3.amazonaws.com/photos/cafe-interior.jpg",
    "https://s3.amazonaws.com/photos/latte.jpg"
  ],
  "loc": {
    "lat": 39.9342,
    "lng": 116.4478,
    "name": "Sanlitun SOHO"
  },
  "createdAtMs": 1699268400000
}
```

#### 2. Create Map Post (Selected Friends Visible)

**Client sends creation request with specific members**:
```json
{
  "type": "MAP_POST_CREATE",
  "clientReqId": "test-uuid-67890",
  "title": "Private Dinner Spot",
  "description": "Weekend gathering location",
  "mediaUrls": ["https://s3.amazonaws.com/photos/restaurant.jpg"],
  "loc": {
    "lat": 39.9088,
    "lng": 116.3975,
    "name": "Gongti Restaurant"
  },
  "allFriends": false,
  "memberIds": [101, 102, 103]
}
```

> **Note**: When `allFriends` is `false`, you must provide a non-empty `memberIds` array. The creator will be automatically added to the member list.

#### 3. Map Post Without Media

```json
{
  "type": "MAP_POST_CREATE",
  "clientReqId": "test-uuid-99999",
  "title": "Sam's Club Sale",
  "description": "Beef 50% off today!",
  "mediaUrls": [],
  "loc": {
    "lat": 39.9042,
    "lng": 116.4074,
    "name": "Chaoyang Joy City"
  },
  "allFriends": true,
  "memberIds": []
}
```

#### 4. Error Scenarios

**Missing location information**:
```json
{
  "type": "MAP_POST_CREATE",
  "clientReqId": "error-test-1",
  "title": "Test Post",
  "description": "Missing location",
  "mediaUrls": [],
  "allFriends": true,
  "memberIds": []
}
```
> **Response**: `{"type":"ERROR","msg":"Location (lat, lng) is required"}`

**Empty memberIds when allFriends is false**:
```json
{
  "type": "MAP_POST_CREATE",
  "clientReqId": "error-test-2",
  "title": "Test Post",
  "description": "No members specified",
  "mediaUrls": [],
  "loc": {"lat": 39.93, "lng": 116.44, "name": "Test"},
  "allFriends": false,
  "memberIds": []
}
```
> **Response**: `{"type":"ERROR","msg":"memberIds cannot be empty when allFriends is false"}`

**Invalid coordinates**:
```json
{
  "type": "MAP_POST_CREATE",
  "clientReqId": "error-test-3",
  "title": "Test Post",
  "description": "Invalid coordinates",
  "mediaUrls": [],
  "loc": {"lat": 999.99, "lng": 116.44, "name": "Test"},
  "allFriends": true,
  "memberIds": []
}
```
> **Response**: `{"type":"ERROR","msg":"Invalid latitude or longitude"}`

#### 5. Map Post Architecture

When a Map Post is created:
- A `map_posts` record is created with location data and content
- A corresponding `conversation` (conv_type=3) is created as the comment section
- Members are added to `conversation_members` based on visibility settings
- The `convId` is returned, allowing users to comment via the standard messaging system

**Database Tables**:
- `map_posts`: Stores post content, location (lat/lng/geohash), and statistics
- `conversations`: Stores the comment section (conv_type=3)
- `conversation_members`: Stores who can view and comment on the post
- `map_post_likes`: (Optional) Stores like records

**Field Validations**:
- `title`: Required, non-empty string
- `loc.lat`: Required, range [-90, 90]
- `loc.lng`: Required, range [-180, 180]
- `allFriends`: Required, boolean
- `memberIds`: Required if `allFriends` is `false`, must be non-empty array
- `mediaUrls`: Optional, array of pre-uploaded S3 URLs

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

