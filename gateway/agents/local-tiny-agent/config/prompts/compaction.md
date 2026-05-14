## Task Context
- Context limit or auto-compaction was reached in a Local Tiny Agent session.
- This agent uses a local small LLM and must keep continuation context very small.

## Compression Budget
- Target summary size: 1K-2K tokens.
- Hard upper bound: 4K tokens.
- Prefer underestimating. The next turn adds prompts, tools, user text, and new tool results.
- Do not copy long command output, logs, fetched pages, or file excerpts.

## Conversation History
{{ messages }}

Wrap reasoning in `<analysis>` tags:
- Identify the latest user request.
- Preserve only facts needed to continue the current task.
- Preserve exact paths, URLs, command names, error codes, and short evidence snippets.
- Preserve tool calls already made and their useful results.
- Drop unrelated prior turns, repeated output, raw logs, and verbose narration.

### Include the Following Sections
1. **Current User Goal** - the latest task in one or two sentences.
2. **Constraints** - active limits, allowlist failures, user preferences, or safety boundaries.
3. **Evidence Collected** - tool calls and concise results that still matter.
4. **Open Questions** - what remains unknown.
5. **Next Step** - the one direct next action.

Keep this summary dense, factual, and continuation-focused. It is for the next agent turn, not for the user.
