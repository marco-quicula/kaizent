---
name: diagnose-stack-trace
description: Reads a Java stack trace or startup log from PetClinic, finds the root exception and matches it to known issues or the setup documentation. Use when a ticket contains a stack trace, an exception or a failed startup.
---
# Diagnose a stack trace

1. Find the root cause: the last `Caused by:` line, or the first exception when there is no
   cause. Note its class and message.
2. Find the first frame in PetClinic's own code (`org.springframework.samples.petclinic`),
   if any, to see what the application was doing.
3. Match the root cause:
   - against `known-issues/`, for errors seen while using the application;
   - against `setup/`, for startup and connection errors (profiles, databases, ports).
4. Explain the cause in one or two sentences, then give the fix as concrete steps or
   commands from the knowledge base.
5. If the trace is cut or the cause is not in the knowledge base, say what else is needed
   (the full log, the start command, the active profile).
6. Cite the documents you used.
