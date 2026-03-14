# recruit-agent

An agent-oriented recruitment backend for resume ingestion, candidate retrieval, multi-turn refinement, comparison, and interviewer handoff generation.

## Overview

This project is built around a realistic recruiting workflow instead of a single search API.

It supports:
- resume upload, parsing, OCR fallback, and indexing
- structured chunking for retrieval-friendly resume evidence
- hybrid candidate retrieval with keyword, profile-vector, and chunk-vector recall
- JD-aware reranking
- multi-turn chat search with refinement inheritance
- candidate comparison from ranked results
- interviewer handoff briefs based on retrieved resume evidence

## Core Workflow

### 1. Resume Ingestion
- upload PDF resumes
- extract text with PDF parsing and OCR
- normalize text
- split resumes into structured chunks
- build:
  - `resume_chunk`
  - `candidate_profile`

### 2. Retrieval
- natural language query parsing
- query residual plus structured filter extraction
- hybrid retrieval:
  - keyword retrieval
  - `candidate_profile` vector retrieval
  - `resume_chunk` vector retrieval
- rerank for final ordering

### 3. JD-Aware Search
- chat sessions can bind a `positionId`
- `PositionJD` constraints are injected into search
- JD-aware reranking uses:
  - `title`
  - `prioritySkills`
  - `bonusSkills`

### 4. Multi-turn Agent Workflow
- `SEARCH`
- `FILTER_REFINE`
- `COMPARE`
- `INTERVIEW`

The chat workflow supports both:
- append-style refinement, for example continuing to narrow results by adding new constraints
- reset-style refinement, for example rerunning screening from scratch with a new set of constraints

## Main Capabilities

### Resume Upload and Indexing
- `POST /api/resumes/upload`

### Candidate Search
- `POST /api/search/candidates`
- `POST /api/search/candidates/refine`

Supported filter dimensions:
- degree
- school tier
- years of experience
- technical skills
- city
- big tech
- outsourcing
- career stage

### Chat Search
- `POST /api/chat`
- `POST /api/chat/stream`

### Candidate Comparison
- `POST /api/comparison/candidates`

Supports selecting candidates by ranked result position, such as:
- first two
- top three
- last three
- discrete selections like `1, 4, 5, 8`

### Interview Handoff Brief
- `POST /api/interview/questions`

This is not framed as “HR generates technical interview questions”.
It is framed as an interviewer handoff brief containing:
- recommendation reasons
- risks and gaps
- follow-up directions

Evidence is selected from high-signal resume sections:
- project experience
- work experience
- internship experience
- skills

## Tech Stack

- Java 17
- Spring Boot 3.4.x
- Spring AI
- MySQL 8
- Elasticsearch 8
- PDFBox
- PaddleOCR
- BGE-M3 embedding
- bge-reranker-v2-m3

## Project Structure

```text
src/main/java/com/recruit/agent
├── agent
├── candidate
├── chat
├── common
├── comparison
├── interview
├── position
├── rag
├── resume
└── search

python
├── ocr-service
├── embedding-service
└── rerank-service
```

## Local Setup

### 1. Start Infrastructure

```powershell
docker compose up -d
```

Default ports:
- MySQL: `3307`
- Elasticsearch: `9200`
- Kibana: `5601`

### 2. Start Local Model Services

OCR:

```powershell
cd python/ocr-service
.\install.ps1
.\start.ps1
```

Embedding:

```powershell
cd python/embedding-service
.\install.ps1
.\start.ps1
```

Rerank:

```powershell
cd python/rerank-service
.\install.ps1
.\start.ps1
```

### 3. Start the Application

```powershell
$env:OPENAI_API_KEY='dummy'
.\mvnw.cmd -gs global-settings.xml -s settings.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

Notes:
- `local` profile is wired to OCR, embedding, and rerank services
- a placeholder `OPENAI_API_KEY` is still recommended for startup compatibility

### 4. Run Tests

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml test
```

## Recommended Demo Flow

### Search Demo
1. `Java 后端`
2. `搜索 推荐 Java`
3. `Go 微服务`
4. `前端 React`
5. `985 硕士 Java`

### JD Chat Demo
Use:
- `positionId = JD-DEMO-001`

Then:
1. search for Java backend candidates
2. refine to 985/master candidates
3. continue refining by city
4. compare the top candidates
5. generate an interviewer handoff brief

## Project Positioning

Mainline:
- OCR and structured chunking
- hybrid retrieval
- rerank
- JD-aware search
- compare

Extension workflows:
- chat orchestration
- interviewer handoff brief

At this stage, the project is better served by:
- stabilizing demos
- improving interview storytelling
- keeping docs aligned with actual runtime behavior
