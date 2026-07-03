(ns teller-services.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [teller-services.actor :as actor]
            [teller-services.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-account! st {:account-id "account-1" :name "J. Alvarez"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:account-id "account-1" :op :process :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "account-1"))))))

(deftest holds-on-unregistered-account-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:account-id "no-such-account" :op :process :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-account")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; above-threshold disbursement always escalates (governor invariant)
        request {:account-id "account-1" :op :disburse-above-threshold :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "account-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "account-1")))))))
