# Jharkhand Civic Innovation & Problem Resolution Platform

Production-quality SIH/demo-ready civic-tech prototype connecting citizens, Jharkhand Government Departments, Higher Education Institutions and Industry partners to move real civic problems from submission to measurable impact.

## What is implemented

- React + TypeScript + Vite + Tailwind CSS frontend
- Java 21 + Spring Boot REST backend
- Exactly five primary roles: USER, ADMIN, DEPARTMENT, INSTITUTION, INDUSTRY
- JWT + BCrypt role-based authentication
- JSON persistence with repository abstraction; no database
- xAI Grok 4.6 through the backend Responses API with structured JSON output
- Deterministic, explainable routing separated from AI analysis
- Admin human validation before routing
- Duplicate detection using deterministic text similarity
- Text, image, voice/audio and document problem evidence
- Browser speech-to-text and text-to-speech abstractions
- English/Hindi UI plus scalable fallback files for Santhali, Nagpuri, Kurukh, Mundari and Khortha
- Institution Tier 1–4 matching with limited tier influence
- Project lifecycle, milestones, field-testing, impact verification
- Later-stage industry collaboration only
- Civic points, notifications and audit logs
- Responsive government/civic-tech UI with light green theme
- Swagger/OpenAPI
- Docker deployment configuration

## Source-of-truth seed data

The initial department and institution reference data is derived from the uploaded source PDFs. The department source contains 36 department records; the institution source PDFs contain 15 Tier 1, 15 Tier 2, 16 Tier 3 and 20 Tier 4 records. Institution descriptions and expertise are preserved from those source datasets rather than invented.

## Architecture

React Frontend → Spring Boot REST API → Application Services → AI Analysis Service → xAI Grok

Application Services → Deterministic Routing Engine → JSON Repositories → Jharkhand reference datasets

AI understands the problem. The routing engine makes a transparent matching recommendation. Admin makes the final human decision.

## No database

The prototype deliberately does not use PostgreSQL, MySQL, MongoDB, Redis, Firebase, Supabase, vector databases, PostGIS, Kafka, Kubernetes, a Python ML backend or a second LLM.

Reference JSON is treated as read-only seed/reference data. Demo application JSON files are separate. Repository interfaces keep the service layer migration-ready.

> This prototype uses JSON-based persistence instead of a database for rapid deployment and demonstration. Production deployment should migrate transactional data to a persistent database.

Also note: deployed serverless/container filesystems may be ephemeral. Persistent user uploads and transactional records should move behind a pluggable storage/repository implementation for production.

## Workflow

SUBMITTED → AI_ANALYZED → PENDING_VALIDATION → VALIDATED → ROUTED → ACCEPTED → COLLABORATION_OPEN → TEAM_FORMED → RESEARCH → PROTOTYPE → FIELD_TESTING → VALIDATION → DEPLOYED → IMPACT_TRACKING → COMPLETED

Additional states: REJECTED, CANCELLED, NEEDS_CLARIFICATION, ON_HOLD.

Industry is intentionally excluded from initial routing and enters only during project collaboration, funding, prototype, testing and deployment.

## Routing

The deterministic engine combines:

- Expertise match
- Problem/domain compatibility
- Location suitability
- Capacity factor
- Limited institution tier bonus

The UI shows matching reasons. Tier never automatically wins a recommendation.

## AI

Set `XAI_API_KEY` on the backend only. Never expose it through Vite environment variables. The backend calls `https://api.x.ai/v1/responses` with `grok-4.6` and structured JSON output. If Grok is unavailable, the problem remains saved in a pending state and the admin can retry or manually validate/classify.

## Local setup

### Backend

From the repository root:

```bash
export XAI_API_KEY=your_key
export JWT_SECRET=replace-with-a-long-random-secret
mvn -f backend/pom.xml spring-boot:run
```

Backend defaults to `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Create `frontend/.env.local` if needed:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## Demo accounts

All seeded demo accounts use password `password`.

- user@jharkhand.gov.demo
- admin@jharkhand.gov.demo
- department@jharkhand.gov.demo
- institution@jharkhand.gov.demo
- industry@jharkhand.gov.demo

## Deployment

### Frontend

Deploy the repository as a Vite project on Vercel using the included `vercel.json`. The React Router SPA fallback is configured so direct navigation to `/login`, `/dashboard`, etc. does not become a Vercel 404.

**Required Vercel environment variable:** `VITE_API_BASE_URL=https://YOUR-BACKEND-DOMAIN/api` (replace with the public Spring Boot backend URL). Vite embeds `VITE_*` variables at build time, so set this variable before deploying/redeploying the frontend. Do not put `XAI_API_KEY` or `JWT_SECRET` in the frontend.

### Backend

The repository includes both `Dockerfile.vercel` at the root and `backend/Dockerfile.vercel`. The root Dockerfile copies the backend artifact and JSON data into the image. The server listens on `$PORT` and reads secrets from environment variables.

Required backend variables:

```env
XAI_API_KEY=
XAI_MODEL=grok-4.6
FRONTEND_URL=
JWT_SECRET=
PORT=8080
```

## API groups

Authentication, problems, AI analysis, routing, departments, institutions, projects, milestones, industry opportunities, collaborations, feedback, impact, civic points and admin governance APIs are included. Swagger is available at `/swagger-ui.html` when the backend is running.

## Limitations intentionally kept for prototype reliability

- Prototype image classification is a replaceable service and does not claim a trained ML model.
- Browser speech recognition depends on browser support.
- Browser speech synthesis is used initially.
- Map data uses simple latitude/longitude; no complex GIS is required.
- JSON writes are suitable for local/demo use, not concurrent production transactions.
- Digital collaboration documents are status-tracked but are not legally binding signatures.


## Final implementation audit

A second pass was performed against the supplied 52-section specification and acceptance checklist. The implementation now includes the previously easy-to-miss governance and workflow pieces: manual Admin classification when Grok is unavailable, mandatory human approval before routing, routing approval/rejection, Department accept/reject/clarification actions, Institution project accept/reject actions, nine default project milestones, field-test evidence upload, project research/prototype file upload, configurable impact metrics, civic-point approval, leaderboard data, in-app notification generation, audit logging, GPS/map display, Admin governance controls, translation override storage, industry-partner management endpoints, and role-based access restrictions on private problem data.

The prototype intentionally keeps Grok analysis separate from deterministic routing. Grok never assigns the final department or institution. Industry is never considered in initial routing.

## Verification performed in the build environment

- All JSON datasets were parsed successfully.
- Java source was statically checked for balanced structure and duplicate controller mappings.
- The frontend TypeScript project was checked with the installed TypeScript compiler; dependency-resolution errors are expected until `npm install` is run because this archive intentionally does not contain `node_modules`.
- Full Maven compilation and npm dependency installation could not be completed in this isolated environment because external package/DNS access was unavailable. The project therefore includes deterministic verification commands for the target machine/CI.

Run before deployment:

```bash
# backend
mvn -f backend/pom.xml clean test

# frontend
cd frontend
npm install
npm run verify
```

## Important deployment note

The project has no database by design. JSON is appropriate for the SIH/demo prototype, but Vercel/serverless runtime filesystems are not a durable transactional store. For production, keep the repository/service interfaces and replace only the persistence/file-storage implementations with a persistent database and object storage.

## Acceptance checklist

The implementation is structured around the supplied acceptance criteria: five roles, multilingual UI, problem submission, text/image/voice, AI structured analysis, duplicate signals, human validation, explainable routing, project lifecycle, later-stage industry collaboration, milestones, field testing, deployment, impact, civic points, notifications, audit logs, JSON persistence, secure Grok credentials, responsive UI and Vercel deployment configuration.
