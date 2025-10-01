# CS407 Final Project Server

## Introduction

This is the backend server for the CS407 Final Project. For detailed project information, please refer to our [Project Proposal](./CS407_Project_Proposal.pdf).

## Development Setup

### Prerequisites
- Docker and Docker Compose
- Java 17
- Maven

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
   # You can check the status with:
   docker-compose ps
   ```

3. **Start the Spring Boot application**
   ```bash
   # Run the application
   mvn spring-boot:run
   ```

4. **Verify everything is working**
   - Spring Boot app: http://localhost:8080
   - API documentation: http://localhost:8080/swagger-ui.html

### Service Ports

- **Spring Boot Application**: http://localhost:8080
- **MySQL Database**: localhost:3306
- **Redis Cache**: localhost:6379

### Database Information

- **Database Name**: cs407_final_project
- **Username**: root
- **Password**: (empty)
- **Alternative User**: app_user (also no password)

### Development Tools

You can use any MySQL client to connect to the database:
- **Host**: localhost
- **Port**: 3306
- **Database**: cs407_final_project
- **Username**: root
- **Password**: (leave empty)

### Stopping the Environment

```bash
# Stop all services
docker-compose down

# Stop and remove data volumes (this will delete all data!)
docker-compose down -v
```

### Database Schema

The database schema is managed by Flyway migrations located in `src/main/resources/db/migration/`. 
When the application starts, it will automatically run any pending migrations.

### Troubleshooting

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

### Current Project Structure

```
knot_server/
├── src/main/
│   ├── java/com/example/knot_server/    # Java source code
│   └── resources/
│       ├── application.yml              # Application configuration
│       └── db/migration/                # Flyway database migrations
├── docker/
│   └── mysql/init/                      # MySQL initialization scripts
├── docker-compose.yml                  # Docker services configuration
└── pom.xml                             # Maven dependencies
```