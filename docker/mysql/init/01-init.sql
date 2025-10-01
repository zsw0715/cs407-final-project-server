-- 📝 01-init.sql 文件说明
-- 这个文件是MySQL容器的数据库初始化脚本，作用是：

-- 🗄️ 自动创建数据库: 当MySQL容器首次启动时自动创建cs407_final_project数据库
-- 👤 配置用户权限: 设置root用户可以无密码从任何地址连接
-- 🔐 创建应用用户: 创建专用的app_user用户
-- ⚡ 即时生效: 容器启动后立即可用，无需手动设置

-- Database initialization script
-- This script will be executed automatically when the MySQL container starts for the first time

-- Ensure the database exists
CREATE DATABASE IF NOT EXISTS cs407_final_project CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant root user permission to connect from any host (without password)
ALTER USER 'root'@'localhost' IDENTIFIED BY '';
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

-- Create application user and grant permissions
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON cs407_final_project.* TO 'app_user'@'%';

-- Refresh privileges
FLUSH PRIVILEGES;

-- Switch to the target database
USE cs407_final_project;

-- You can add some initial table structures here if needed
-- Flyway will handle database migrations, so usually only basic setup is needed here

SELECT 'Database cs407_final_project initialized successfully!' as message;