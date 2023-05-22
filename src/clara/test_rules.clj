(ns clara.test-rules
  (:require [clara.rules :refer [defrule insert!]]
            [clara.rules.accumulators :as acc]))

(defrecord WindSpeed [location])

(defrecord Temperature [location])

(defrecord UnpairedWindSpeed [windspeed])

(defrule find-wind-speeds-without-temp
  "Rule using NegationNode"
  [?w <- WindSpeed
   (not= this ())
   (= ?loc location)]
  [:not [Temperature (= ?loc location)]]
  =>
  (insert! (->UnpairedWindSpeed ?w)))

(defrule income-wage-earner-context
  "Defines the income wage earner who is currently being employed"
  [:loan/borrower [{:keys [borrower-id]}] (= borrower-id ?borrower-id)]
  [:loan/borrower-employer [{:keys [borrower-id self-employed? status employer-id start-date]}]
   (= borrower-id ?borrower-id)
   (= status "Current")
   (not self-employed?)
   (= employer-id ?employer-id)
   (= start-date ?start-date)]
  [?income-type-set <- (acc/distinct :type) :from [:loan/borrower-income-item [{:keys [borrower-id employer-id]}]
                                                   (= borrower-id ?borrower-id)
                                                   (= employer-id ?employer-id)]]
  [:test (seq ?income-type-set)]
  =>
  (doseq [income-type ?income-type-set]
    (insert! {:fact/type :context/income-wage-earner-current-employment
              :borrower-id ?borrower-id
              :employer-id ?employer-id
              :start-date  ?start-date
              :income-type income-type})))
