(ns teller-services.governor
  "TellerServicesGovernor — the independent safety/traceability layer
  for the ISCO-08 4211 independent teller-services actor. Wired as
  its own `:govern` node in `teller-services.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of account
  provenance or disbursement/identity-override risk, so this MUST be
  a separate system able to reject a proposal (itonami actor pattern,
  per ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. account provenance  — the request's account must be
       registered.
    2. no-actuation          — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: cash disbursement above a threshold and
  identity-verification overrides always require human sign-off):
    3. :op :disburse-above-threshold.
    4. :op :override-identity-verification.
    5. low confidence (< `confidence-floor`)."
  (:require [teller-services.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:disburse-above-threshold :override-identity-verification})

(defn- hard-violations [{:keys [proposal]} account-record]
  (cond-> []
    (nil? account-record)
    (conj {:rule :no-account :detail "未登録 account"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `teller-services.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [account-record (store/account store (:account-id request))
        hard (hard-violations {:proposal proposal} account-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
