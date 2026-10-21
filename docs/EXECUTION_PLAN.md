# 📋 SyncFlow: Master Step-by-Step Execution Plan

> **Objective**: Build and deploy **SyncFlow** — an enterprise-grade collaborative project management and team operations platform using a 100% free, highly scalable Windows ecosystem (Angular 18 + Ionic/Capacitor, Spring Boot 3.x with Java 21 Virtual Threads, MSSQL Express, WebSocket, Jitsi Meet, and Cloudflare Tunnel).

---

## 🗺️ High-Level Project Roadmap

```mermaid
gantt
    title SyncFlow Implementation Timeline
    dateFormat  YYYY-MM-DD
    section Foundation
    Phase 1: Project Scaffolding & DB Setup       :active, p1, 2026-10-22, 2d
    Phase 2: Security & Dynamic RBAC Engine       :p2, after p1, 2d
    section Core Features
    Phase 3: Pipeline Builder & Kanban Engine     :p3, after p2, 3d
    Phase 4: Real-Time Chat & Jitsi Meetings     :p4, after p3, 3d
    Phase 5: Executive Dashboard & Analytics      :p5, after p4, 2d
    section Production
    Phase 6: Mobile APK & Windows 24/7 Hosting   :p6, after p5, 2d
```

---

## 📌 Phase 1: Environment & Project Scaffolding
**Goal**: Set up the database, initialize Spring Boot backend, and bootstrap the Angular 18 + Ionic client.

- [ ] **1.1 Database Initialization (MSSQL Express)**
  - Create database `syncflow_db` on local SQL Server instance.
  - Create application service user `syncflow_app` with appropriate table/schema permissions.
  - Create T-SQL initial schema scripts.
- [ ] **1.2 Backend Scaffolding (`/backend`)**
  - Initialize Spring Boot 3.3.x with Java 21.
  - Configure `pom.xml` with dependencies:
    - `spring-boot-starter-web` (Virtual Threads enabled: `spring.threads.virtual.enabled=true`)
    - `spring-boot-starter-security`
    - `spring-boot-starter-data-jpa`
    - `com.microsoft.sqlserver:mssql-jdbc`
    - `spring-boot-starter-websocket`
    - `com.github.ben-manes.caffeine:caffeine`
    - `io.jsonwebtoken:jjwt-api:0.12.5`
    - `org.projectlombok:lombok`
  - Setup Maven Wrapper (`mvnw.cmd`) and verify build.
- [ ] **1.3 Frontend Scaffolding (`/frontend`)**
  - Scaffold Angular 18 application with Standalone Components & Signals.
  - Install Ionic Angular UI (`@ionic/angular`) and Capacitor (`@capacitor/core`, `@capacitor/cli`).
  - Configure **"Emerald Reserve"** light design system in `src/styles/variables.scss` (`#006022` primary, `#F6F8F6` canvas, `#FFFFFF` cards, `#EFF4F0` sidebars).
  - Verify local dev server running via `npm.cmd start`.

---

## 📌 Phase 2: Authentication & Dynamic RBAC Engine
**Goal**: Complete invite-only authentication and dynamic checkbox permission matrix.

- [ ] **2.1 Entity & Schema Layer**
  - Implement `User`, `Role`, `Permission`, `UserPermissionOverride` JPA entities.
  - Seed system roles: `REPORTING_MANAGER`, `TEAM_LEAD`, `DEVELOPER`, `SQA`, `OBSERVER`.
  - Seed granular permissions (`PROJECT:CREATE`, `TASK:APPROVE`, `STAGE:BUILD`, `MEETING:HOST`, etc.).
- [ ] **2.2 Spring Security 6 & Evaluator**
  - JWT Token generation, validation filter, and authentication entry point.
  - Implement `CustomPermissionEvaluator` supporting `@PreAuthorize("hasPermission(#id, 'PROJECT', 'EDIT')")`.
  - Integrate Caffeine In-Memory Cache for $<1\text{ms}$ permission checks.
- [ ] **2.3 Angular Auth & Permissions UI**
  - Premium light-themed Login page with bank branding.
  - Angular Functional Route Guards (`authGuard`, `permissionGuard`).
  - Structural Directive `*hasPermission="'TASK:EDIT'"` for dynamic UI control.
  - Admin RBAC Management Matrix: Interactive checkbox table to customize role permissions & individual user overrides.

---

## 📌 Phase 3: Core Workspace, Custom Pipeline & Kanban Engine
**Goal**: Enable dynamic stage building, drag-and-drop Kanban, blocker alerts, and dependencies.

- [ ] **3.1 Projects & Team Management**
  - REST endpoints for project creation, team member assignments, and deadline settings.
  - Reporting Manager project lifecycle management (`Active`, `On Hold`, `Completed`).
- [ ] **3.2 Team Lead Custom Pipeline Builder**
  - Dynamic stage management API (add, reorder, rename stages: Backlog $\rightarrow$ DB $\rightarrow$ API $\rightarrow$ UI $\rightarrow$ Review $\rightarrow$ Done).
  - Pipeline stage drag-and-drop configuration interface.
- [ ] **3.3 Interactive Kanban Board**
  - Task cards with priority badges (`Low`, `Medium`, `High`, `Critical`), assignees, and progress slider (0–100%).
  - Smooth card drag-and-drop across stages using Angular CDK.
  - Real-time stage sync via WebSocket broadcast (`/topic/project/{id}/kanban`).
- [ ] **3.4 "Raise a Blocker" System**
  - Blocker toggle with reason input modal on task cards.
  - Instant high-priority broadcast to Manager & Team Lead boards with rose crimson alert (`#E11D48`).
- [ ] **3.5 Task Dependency Engine**
  - Define prerequisites between tasks.
  - Auto-notification trigger when blocking task is completed.

---

## 📌 Phase 4: Real-Time Discord-Style Chat & Jitsi Video Meeting
**Goal**: High-speed internal team communication and instant meetings without external tool subscriptions.

- [ ] **4.1 WebSocket STOMP Infrastructure**
  - Configure Spring WebSocket broker with SockJS fallback at `/ws-syncflow`.
  - JWT authentication handshake interceptor for secure socket sessions.
- [ ] **4.2 Discord-Style Channel Experience**
  - Global `#general` channel + Project-specific topic channels (`#api-discussion`, `#ui-bugs`).
  - Real-time message streaming with rich text, code snippets, and file attachments.
- [ ] **4.3 Read Receipts ("Seen by...")**
  - Track message delivery and viewed status.
  - Real-time UI indicator showing team member avatars who viewed the message.
- [ ] **4.4 Jitsi Meet Integration**
  - One-click instant meeting launcher (generates secure room IDs).
  - Embedded Jitsi IFrame API inside custom Angular modal styled with SyncFlow theme.
  - Screen sharing, in-call chat, and participant tile controls.

---

## 📌 Phase 5: 360° Executive Dashboard, Analytics & Productivity
**Goal**: Executive visibility, team analytics, and automated productivity logging.

- [ ] **5.1 360° Live Executive Dashboard**
  - Visual completion charts, stage velocity, and project risk indicators.
  - High-level manager review & task approval queue.
- [ ] **5.2 Developer Performance Analytics**
  - **On-Time Delivery Rate**: Percentage of tasks completed before deadlines.
  - **Quality / Approval Score**: Ratio of first-pass approvals vs. rework revisions.
  - **Workload Heatmap**: Real-time visualization of task loads per developer.
- [ ] **5.3 Leave & Availability System**
  - `On Leave` badge with return date indicator.
  - Assignment warning modal preventing accidental overload of team members on leave.
- [ ] **5.4 Automated Daily Work Log**
  - Background cron generating daily work summaries based on completed tasks, comments, and slider changes.

---

## 📌 Phase 6: Mobile APK Packaging & Windows 24/7 Hosting
**Goal**: Package the Android APK and deploy the entire solution live on Windows Server at zero software cost.

- [ ] **6.1 Capacitor Android Mobile App**
  - Generate Android project via Capacitor CLI: `npx cap add android`.
  - Configure responsive navigation bar, hardware back button, and status bar styling.
  - Build signed production APK.
- [ ] **6.2 Firebase Cloud Messaging (FCM Free Tier)**
  - Integrate FCM Android SDK for background task notifications and meeting alerts.
  - Backend Spring Boot FCM client using Firebase Admin SDK.
- [ ] **6.3 Windows Server 24/7 Background Service (WinSW)**
  - Configure `WinSW.xml` to run Spring Boot `.jar` automatically on system boot.
  - Verify automated restarts and roll-logging.
- [ ] **6.4 Nginx Reverse Proxy & Cloudflare Tunnel**
  - Configure Nginx for Windows to proxy REST API and WebSocket connections.
  - Set up `cloudflared.exe` tunnel for free HTTPS domain mapping (`https://syncflow.yourdomain.com`) with zero router port forwarding.

---

## 🎯 Immediate Next Step
Execute **Phase 1**:
1. Create `/backend` directory and bootstrap Spring Boot 3.3.x with Java 21 and MSSQL configuration.
2. Create `/frontend` directory and bootstrap Angular 18 with Ionic and "Emerald Reserve" design tokens.
