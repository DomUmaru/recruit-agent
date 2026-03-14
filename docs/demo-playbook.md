# Demo Playbook

## Demo Goal

Use the smallest possible sequence to show three things:
- the system can find the right candidates
- the system can refine results under a JD context
- the system can continue into compare and interviewer handoff

## Prerequisites

Make sure these are running:
- MySQL
- Elasticsearch
- OCR service
- embedding service
- rerank service
- Spring Boot app

Recommended clients:
- Apifox
- Postman

Use UTF-8 JSON for Chinese chat requests.

## Demo Route A: Search

### Step 1
`POST /api/search/candidates`

Query:
- `Java 后端`

What to say:
- this proves the system can do base candidate retrieval
- results include evidence, not just names
- the ranking is not pure keyword matching

### Step 2
Query:
- `搜索 推荐 Java`

What to say:
- this proves the system can distinguish direction-specific backend profiles

### Step 3
Query:
- `985 硕士 Java`

What to say:
- semantic topic and structured filters are separated
- `Java` remains the retrieval theme
- `985` and `硕士` become filters

## Demo Route B: JD Chat

Use:
- `positionId = JD-DEMO-001`

### Step 1
`帮我找 Java 后端候选人`

What to say:
- the chat session is bound to a JD
- the system first loads JD defaults
- ranking includes JD-aware preference

### Step 2
`只看985硕士`

What to say:
- this is append-style refinement
- it narrows the previous result set

### Step 3
`继续只看北京的`

What to say:
- the original search theme is inherited
- only the filters continue to narrow

### Step 4
`重新帮我筛选一次，这次按上海 Java`

What to say:
- this is reset-style refinement
- previous filter constraints are replaced instead of appended

### Step 5
`帮我比较前两个候选人`

What to say:
- compare consumes the current result set directly
- the ranked list is explicitly addressable by position

### Step 6
`给第2个候选人生成面试交接提纲`

What to say:
- this is not “HR asks AI to generate technical interview questions”
- this is an interviewer handoff brief
- it includes recommendation reasons, risks, and follow-up directions

## Demo Notes

- For Chinese multi-turn refinement, always use UTF-8 clients like Apifox
- `careerStage` is not treated as a default hard JD filter yet
- compare and interview are extension workflows, not the main retrieval chain
