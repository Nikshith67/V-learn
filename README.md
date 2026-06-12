# V-Learn – E-Learning Platform

A full-stack e-learning web application built with **Java**, **Spring Boot**, **Spring Data JPA**, and **Thymeleaf**.  
Authentication and role-based access are implemented with **HttpSession** and **Spring Web Interceptors** (no Spring Security).

## Features

- **Roles:** Administrator, Teacher, Student  
- **Admin:** Approve teacher registrations, view system metrics  
- **Teacher:** Upload videos (subject, title, description, .mp4), add MCQ questions (4 options, 1 correct) per video  
- **Student:** Browse videos by subject, watch with 80% progress tracking, take timed MCQ tests (25 sec/question), view results and profile  

## Tech Stack

- Java 21, Spring Boot 4, Spring Data JPA, Thymeleaf  
- H2 (dev), optional MySQL  
- Tailwind CSS (CDN), dark theme  

## Run the Application

```bash
mvn spring-boot:run
```

Open: **http://localhost:8080**

## Default Admin

- **Username:** `admin`  
- **Password:** `admin123`  

Use this to log in and approve teacher accounts.

## Logo

Replace `src/main/resources/static/images/logo-placeholder.png` with your own PNG. The app uses the image with ID `app-logo` and `src="/images/logo-placeholder.png"` in the navbar and result views.

## Configuration

- **H2:** `application.properties` (file-based DB under `./data/vlearn`)  
- **Videos:** Stored under `vlearn.videos.upload-dir` (default: `./vlearn-uploads/videos`)  
- **MySQL:** Uncomment and set the datasource/JPA properties in `application.properties` for production  

## Project Layout

- **Entities:** `User`, `Video`, `Question`, `TestResult`, `VideoProgress`  
- **Controllers:** `AuthController`, `AdminController`, `TeacherController`, `StudentController`, `VideoController`, `TestController`, `ResultController`  
- **Auth:** `AuthInterceptor` + `WebMvcConfig` for role-based access; passwords hashed with a simple SHA-256-based hash (no BCrypt)
