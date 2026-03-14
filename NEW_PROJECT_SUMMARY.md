# NEW_PROJECT_SUMMARY

## Positioning

This is an agent-oriented recruitment backend focused on resume retrieval and candidate screening.

The goal is not just “search resumes by keyword”, but to support a fuller workflow:
- resume ingestion
- candidate search
- multi-turn refinement
- candidate comparison
- interviewer handoff generation
- unified chat interaction

## What Is Already Built

### Data and Indexes
- MySQL for business data
- Elasticsearch for retrieval and evidence

Core indexes:
- `resume_chunk`
- `candidate_profile`

### Ingestion Pipeline
- PDF upload
- PDF text extraction
- OCR fallback / OCR-first local workflow
- text cleanup
- structured chunking
- candidate profile extraction
- MySQL + Elasticsearch persistence

### Search Pipeline
- keyword retrieval
- `candidate_profile` vector retrieval
- `resume_chunk` vector retrieval
- hybrid fusion
- evidence retrieval
- natural language filter parsing
- refinement merge
- rerank

### Agent Pipeline
- router
- state
- tool execution
- orchestrator
- chat API
- SSE stream

### Business Workflows
- search
- refinement
- compare
- interview handoff brief

## Current Assessment

The project is already past the “missing major features” phase.

The current stage is:
- core workflows are closed-loop
- local OCR / embedding / rerank are integrated
- search, compare, JD-context chat, and interview handoff are all runnable
- the highest-value work is now demo quality, stability, and communication

## Main Engineering Takeaways

- Resume chunking quality matters directly to retrieval quality
- Search works better as query residual + structured filters, not raw keyword search
- JD context should shape both filtering and reranking
- Compare and interview workflows are better treated as downstream recruiter/interviewer collaboration tools
- Multi-turn refinement must distinguish between:
  - append refinement
  - reset refinement

## Recommended One-line Pitch

This is a recruitment agent backend that combines resume parsing, structured chunking, hybrid retrieval, reranking, and JD-aware conversation flow to help recruiters and hiring teams find better-matched candidates with evidence-backed results.
