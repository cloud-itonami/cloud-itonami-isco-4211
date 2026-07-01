# cloud-itonami-isco-4211

Open Occupation Blueprint for **ISCO-08 4211**: Bank Tellers and Related Clerks.

This repository designs a forkable OSS business for an independent financial-services teller operating a community teller window: a document-handling robot performs cash-drawer reconciliation support under a governor-gated actor, so the practice keeps its own transaction records instead of renting a closed banking-teller SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-handling robot performs cash-drawer reconciliation support and physical filing of transaction slips under an actor that proposes
actions and an independent **Teller Services Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
cash disbursement above a threshold, or identity-verification overrides) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client request + account verification + transaction limit
        |
        v
Teller Services Advisor -> Teller Services Governor -> process/log, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4211`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
