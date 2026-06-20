# 🏢 RoomSched - 智能会议室预约系统

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg?logo=springboot)
![JDK](https://img.shields.io/badge/JDK-17-orange.svg?logo=java)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.5-blue.svg)
![UI](https://img.shields.io/badge/UI-Vanilla%20CSS%20%2B%20Layui-ff69b4.svg)
![License](https://img.shields.io/badge/License-Proprietary-red.svg)

RoomSched 是一个基于 **Spring Boot 3** 和 **Thymeleaf** 构建的现代化会议室预约管理系统。系统旨在为中小型企业及团队提供高效、美观、便捷的会议空间预订服务。前端设计遵循极简莫兰迪色系（Morandi palette），并原生支持**一键切换深色模式 (Dark Mode)**，确保全天候极佳的视觉体验。

---

## 🌟 核心特性

- **🚀 现代技术栈**：基于 Spring Boot 3.4 + JDK 17，采用 MyBatis-Plus 简化数据持久层开发。
- **🛡️ 强大的安全验证**：集成 Spring Security，提供完善的角色权限分离（管理员/普通用户）。
- **🎨 极简美学 UI**：纯粹的 CSS 设计体系，莫兰迪配色，高级微投影卡片风格，无重度前端框架依赖（基于纯 HTML/CSS + Layui 辅助组件）。
- **🌙 原生暗黑模式**：全站级别的深色模式支持，零侵入式 CSS 变量切换，保障所有组件（如按钮、表格、弹窗）在日夜模式下均有完美展现。
- **📅 智能预约与冲突检测**：实时校验时间段占用情况，彻底杜绝会议室档期冲突。
- **📊 实时状态大屏**：支持会议室资源的全景大屏视图，直观展示当前各会议室的占用情况。
- **📝 全链路操作审计**：完整的管理员控制台，支持对用户、会议室、预约单进行增删改查，并自带审计日志记录所有敏感操作。

---

## 🏗️ 系统架构与目录骨架

系统采用经典的 MVC 三层架构，代码结构清晰，高度解耦：

```text
RoomSched/
├── src/main/java/org/example/roomsched/
│   ├── config/       # 核心配置类（Security, MyBatis-Plus, MVC）
│   ├── controller/   # API 接口与页面路由控制器
│   ├── entity/       # 数据实体类与业务载体 (Entity/VO/DTO)
│   ├── mapper/       # MyBatis-Plus 数据访问层
│   ├── service/      # 核心业务逻辑服务层
│   └── RoomSchedApplication.java # Spring Boot 启动类
├── src/main/resources/
│   ├── db/migration/ # SQL 数据库初始化及表结构更新脚本
│   ├── static/       # 静态资源（自定义高级 CSS、JS 及前端库）
│   ├── templates/    # Thymeleaf 页面模板（按 admin/front 模块化拆分）
│   └── application.yml # 系统全局配置文件
└── pom.xml           # Maven 依赖管理
```

---

## 🛠️ 技术栈清单

### 后端 (Backend)
- **核心框架**：Spring Boot 3.4.1
- **语言**：Java 17
- **模板引擎**：Thymeleaf 3
- **安全框架**：Spring Security 6
- **持久层框架**：MyBatis-Plus 3.5.5
- **数据库**：MySQL 8.x

### 前端 (Frontend)
- **核心**：HTML5, 原生 CSS3 变量驱动设计, Vanilla JavaScript
- **组件库**：Layui (仅用于轻量化交互与表格渲染)
- **弹窗提示**：SweetAlert2
- **图标**：Layui Icons

---

## 🚀 快速开始与傻瓜式体验

为了让您能够零成本、最快速度地体验完整的系统功能，我们已经为您准备好了全套初始化环境。

### 1. 环境准备
- JDK 17 或更高版本
- Maven 3.6+
- MySQL 8.x

### 2. 数据库配置
1. 在 MySQL 中创建名为 `room_sched` 的数据库。
2. 依次运行 `src/main/resources/db/migration/` 目录下的 SQL 脚本（如 `V1__init_schema.sql`、`V2__add_indexes.sql`、`V3__init_admin.sql`）以完成建表与默认数据植入。
3. 修改 `src/main/resources/application.yml` 中的数据库连接密码：
   ```yaml
   spring:
     datasource:
       username: root       # 替换为你的数据库账号
       password: password   # 👉 替换为你的数据库密码
   ```

### 3. 运行与一键体验
在项目根目录下执行以下命令启动服务：
```bash
mvn spring-boot:run
```

- **服务地址**：`http://localhost:8080`
- **👉 默认管理员账号（傻瓜式体验）**：
  - **账号**：`admin`
  - **密码**：`123456`
  > 使用此账号登录后，即可直接进入后台管理面板，体验会议室配置、用户审批及全站暗黑模式联动。

---

## 💡 界面展示 (UI Highlights)
- **浅色模式 (Light Mode)**：干净、透气的纯白背景与石板蓝点缀。
- **暗色模式 (Dark Mode)**：深邃的午夜蓝背景，柔和的灰色文字，保护视力且极具科幻质感。

*点击导航栏右上角的“太阳/月亮”图标即可无缝切换主题！*

---

## 📄 许可证
本项目为专属定制开发系统。保留所有权利。
