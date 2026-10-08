# V-Learn – E-Learning Platform

A full-stack e-learning web application built with **Java**, **Spring Boot**, **Spring Data JPA**, and **Thymeleaf**, enhanced with an **AI Study Assistant** powered by **Google Gemini** / OpenAI-compatible LLMs (Gemini, Groq, NVIDIA NIM, OpenAI).  
Authentication and role-based access are implemented with **HttpSession** and **Spring Web Interceptors** (no Spring Security).

## Features

- **Roles:** Administrator, Teacher, Student  
- **Admin:** Approve teacher registrations, view system metrics  
- **Teacher:** Upload videos (subject, title, description, .mp4), add MCQ questions (4 options, 1 correct) per video  
- **Student:** Browse videos by subject, watch with 80% progress tracking, take timed MCQ tests (25 sec/question), view results and profile  
- **✨ AI Study Assistant:**
  - **Video Summarization:** Generates instant structured summaries of video lessons, core concepts, and key takeaways.
  - **Context-Aware Doubts & Chat:** Ask questions directly tied to the currently selected video lesson and its MCQs, or ask general educational doubts.
  - **Quick Prompts:** Pre-configured one-click questions like "Summarize key concepts", "Explain simply", "Practice questions", and "Real-world examples".
  - **Direct Access from Video Player:** "Ask AI about this video" button on any video watch page to immediately load that lesson's context.

## Tech Stack

- Java 21, Spring Boot 4, Spring Data JPA, Thymeleaf  
- Spring WebFlux (`WebClient`) for non-blocking HTTP calls to AI APIs  
- AI Provider: Google Gemini (default: `gemini-3.5-flash` / OpenAI-compatible endpoint) with high token limits (up to 1M context tokens)  
- H2 (dev), optional MySQL  
- Tailwind CSS & custom design system, dark/light harmonious styling  

## Run the Application

```bash
mvn spring-boot:run
```

Open: **http://localhost:8080**

## Default Credentials

- **Admin:**
  - **Username:** `admin`  
  - **Password:** `admin123`  
- **Sample Student:**
  - **Username:** `student1`  
  - **Password:** `student123`  

Use admin to approve teacher accounts; use student to explore videos and the AI Assistant.

## Logo

Replace `src/main/resources/static/images/logo-placeholder.png` with your own PNG. The app uses the image with ID `app-logo` and `src="/images/logo-placeholder.png"` in the navbar and result views.

## Configuration & Secret Management

### Safe Git & Hosting Setup
To prevent secret API keys from ever being exposed to GitHub:

1. **Local Development (No Git Leaks):**
   Create a local file `src/main/resources/application-local.properties` (already added to `.gitignore`):
   ```properties
   vlearn.ai.api-key=your-gemini-or-openai-api-key
   vlearn.ai.model=gemini-3.5-flash
   vlearn.ai.fallback-model=gemini-3.5-flash-lite
   vlearn.ai.endpoint=https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
   vlearn.ai.max-tokens=4096
   ```

2. **Cloud Hosting (Render, Railway, AWS, Docker, Heroku):**
   In your cloud hosting provider's dashboard, set the environment variable:
   * **`AI_API_KEY`**: Your Gemini API key (e.g. `AIzaSy...`)
   * **`AI_MODEL`** *(optional)*: `gemini-3.5-flash`
   * **`AI_ENDPOINT`** *(optional)*: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`

3. **`application.properties` (Committed to Git):**
   Contains safe placeholders and defaults:
   ```properties
   spring.config.import=optional:classpath:application-local.properties
   vlearn.ai.endpoint=${AI_ENDPOINT:https://generativelanguage.googleapis.com/v1beta/openai/chat/completions}
   vlearn.ai.model=${AI_MODEL:gemini-3.5-flash}
   vlearn.ai.fallback-model=${AI_FALLBACK_MODEL:gemini-3.5-flash-lite}
   vlearn.ai.max-tokens=${AI_MAX_TOKENS:4096}
   vlearn.ai.api-key=${AI_API_KEY:}
   ```

- **H2:** File-based DB under `./data/vlearn`  
- **Videos:** Stored under `vlearn.videos.upload-dir` (default: `./vlearn-uploads/videos`)  
- **MySQL:** Uncomment and set the datasource/JPA properties for production  

## Project Layout

- **Entities:** `User`, `Video`, `Question`, `TestResult`, `VideoProgress`  
- **DTOs:** `ChatMessage` (chat interaction payload)  
- **Services:** `AuthService`, `VideoService`, `TestService`, `AiService` (AI API communication, prompt structuring, markdown responses)  
- **Controllers:** `AuthController`, `AdminController`, `TeacherController`, `StudentController`, `VideoController`, `TestController`, `ResultController`, `AiChatController` (web UI and REST endpoints for `/student/ai`)  
- **Views:** Thymeleaf templates under `src/main/resources/templates/` (including `student/ai-assistant.html`, `student/video-watch.html`, `fragments/sidebar.html`)  
- **Auth:** `AuthInterceptor` + `WebMvcConfig` for role-based access; passwords hashed with a simple SHA-256-based hash

