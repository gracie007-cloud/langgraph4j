# LangGraph4j Agent Guide

This file provides local guidance for coding agents working in this repository.

## Repo map

- `langgraph4j-core/` core library.
- `langgraph4j-*/` optional modules (savers, integrations, etc.).
- `how-tos/` Jupyter notebooks (java kernels [rapaio-jupyter-kernel](https://github.com/padreati/rapaio-jupyter-kernel) ) with examples.
- `studio/` UI and related tooling.

## Build and test

- Build all modules: `./mvnw -q -DskipTests install`
- Run unit tests: `./mvnw -q test`

## Conventions

- Java 17+.
- Keep public APIs stable; prefer additive changes.
- Follow module-local patterns (see existing README in each module).
- Keep logs structured and minimal in production code.
- Minimize third party dependencies

## Before you change code

- Scan nearby modules for similar patterns.
- Prefer `rg` for search.
- Avoid touching unrelated files in a dirty worktree.

## After you change code

- Mention any tests you ran (or didn’t).
- If you add new public APIs, update module README and/or docs in `how-tos/`.
