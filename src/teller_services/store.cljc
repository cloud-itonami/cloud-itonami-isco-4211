(ns teller-services.store
  "SSoT for the ISCO-08 4211 independent teller-services sole-
  proprietor actor. Store is a protocol injected into the
  `teller-services.actor` StateGraph — `MemStore` is the default,
  deterministic, zero-dep backend; a Datomic/kotoba-server-backed
  implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    account  — a registered client account (:account-id, :name)
    record   — a committed operating record under an account
               (processed transaction, log entry, above-threshold
               disbursement, identity-verification override) —
               written ONLY via commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (account [s account-id])
  (records-of [s account-id])
  (ledger [s])
  (register-account! [s account])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (account [_ account-id] (get-in @a [:accounts account-id]))
  (records-of [_ account-id] (filter #(= account-id (:account-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-account! [s account]
    (swap! a assoc-in [:accounts (:account-id account)] account) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:accounts {} :records [] :ledger []} seed)))))
