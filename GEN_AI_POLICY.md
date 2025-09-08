# Generative AI Policy

The Cardinal project accepts contributions created with the assistance of generative AI tooling such as LLMs, with some caveats.
1. A human must be in the loop. Do not under any circumstances send us PRs that haven't been reviewed in full by a human. This is a bannable offense.
2. The buck stops at the first human in the loop. If your LLM tooling writes a bug, I will not treat it any differently from you writing the bug. If the bug is particularly harmful or malicious, you need to be ready to take responsibility for that.
3. Don't vibe-code your core business logic. We don't want to see low-quality AI slop in our geocoder or any of the other core features that define Cardinal maps as an offering in the maps space. If something has been done thousands of times by thousands of apps, we're less concerned about AI-generated boilerplate.

### Suggestions

Every time an agentic change is made:
1. Note which files are affected.
2. Force the agent to read all affected files and examine them for "self-consistency and code quality issues including duplicated logic, dead code and unused stubs".
