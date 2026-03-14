# Interview Talk Track

## One-sentence Version

This project is a recruitment agent backend that combines resume parsing, structured indexing, hybrid retrieval, reranking, JD context, and evidence-backed candidate workflows to help recruiters and hiring managers find better matches faster.

## Why Build It

Real recruiting has several recurring problems:
- resume text quality is unstable
- plain keyword search is weak for both recall and ranking
- recruiters and hiring teams have handoff friction
- after searching, they still need refinement, comparison, and interviewer preparation

So the project was designed as a workflow system, not just a search endpoint.

## How It Works

### Ingestion
- upload resume
- parse PDF / OCR
- clean text
- chunk into structured evidence
- build `resume_chunk` and `candidate_profile`

### Retrieval
- keyword retrieval
- profile vector retrieval
- chunk vector retrieval
- hybrid fusion
- rerank

### JD Context
- chat sessions can bind a `positionId`
- each round can load `PositionJD`
- JD constraints affect filtering and reranking

## What Was Hard

### Chunking
- early chunking was too coarse
- it was rewritten into rule-based section chunking
- chunk enrichment improved retrieval quality

### Chinese Query Parsing
- search needed query residual + structured filters instead of naive keyword search
- multiple fixes were needed around:
  - Chinese encoding
  - residual preservation
  - filter extraction
  - chat refinement inheritance

### Compare and Interview
- compare had to consume ranked candidate sets naturally
- interview was reframed from “question generation” into “interviewer handoff brief”
- evidence selection was changed to use project/work/skill chunks rather than naive first-chunk selection

### Multi-turn Refinement
- the system now distinguishes:
  - append refinement
  - reset refinement
- this was necessary for realistic HR behavior

## Final Outcome

The system now supports:
- search
- refinement
- JD-context chat
- compare
- interviewer handoff brief

The important point is not just that the features exist, but that the main workflows have been run end-to-end with both real and synthetic resume datasets.

## My Role

Recommended phrasing:
- I was responsible for the backend workflow design and implementation, including resume ingestion, index structure, retrieval pipeline, reranking, chat orchestration, and the later quality/stability closeout.
- A lot of the work was not single-point bug fixing but chain-level correction across OCR quality, chunk granularity, query parsing, JD constraints, and evidence alignment.

## If I Continue This Project

I would not keep adding broad new features.

The next sensible steps would be:
- evaluation
- observability
- more robust JD preference modeling
- a fuller business loop around application / candidate / position
