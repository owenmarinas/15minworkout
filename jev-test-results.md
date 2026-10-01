# Jev test results — JevK5 2B vs 4B

Benchmark report from the `At-myserver` session, relayed here as-is.

## Does the 2B coexist with production?

Yes. The 2B uses 2.2 GB of GPU memory against the 4B's 5 GB. The router's local tier was probed
every 20 s during both reruns:

- all 13 probes succeeded, with a median of 0.19 s and a worst of 0.44 s;
- Ollama's model never left the GPU.

With the 4B, the same requests timed out at 60 s.

## Accuracy on real customer messages (bench 2)

| Task | Jev | JevK5 4B | JevK5 2B | local LLM |
|---|---|---|---|---|
| Card-payment triage | 90.0% | 87.8% | 81.1% | 73.3% |
| Transfer triage | 81.7% | 82.2% | 73.9% | 63.3% |
| Lost/stolen yes/no | 94.4% | 87.8% | 88.9% | 73.9% |

- Versus the 4B: 7–8 points worse on the two sorting tasks, about the same on yes/no. It's no
  longer within 3 points of Jev on any task.
- Versus the local LLM: still clearly better, by 8–15 points.
- Order sensitivity: twice the 4B's. It changes 18–27% of answers when the 9 options are listed
  in reverse.
- Speed: about 120 ms per answer, roughly 2.5 times faster than the 4B.

## Routing (bench 1)

It's confident enough to route only 17% of prompts, against the 4B's 47%. Of the prompts it does
route, it gets 91% right, and it resists only a third of injection attempts. It's not viable as
the router's classifier.

## Bottom line

- The 2B: fine as a side-by-side triage helper where "better than the local LLM" is enough. Not
  good enough to replace the router's classifier.
- The 4B: the only open model close to Jev, and it doesn't fit on your current card.

Results are committed (`60d5e88`) in the benchmark's own repo; the server stopped itself when the
runs finished.
