(ns user
  (:require [clj-kondo.hooks-api :as api]
            [hooks.clara-rules :as clara-rules]
            [hooks.gateless-rules :as gateless-rules]))

(def query-node
  (api/parse-string (slurp "resources/clara/foo_query.clj")))

(def rule-node
  (api/parse-string (slurp "resources/clara/foo_rule.clj")))

;; scratch
(comment
  (-> (clara-rules/analyze-defquery-macro {:node query-node}) :node api/sexpr prn)
  (-> (clara-rules/analyze-defrule-macro {:node rule-node}) :node api/sexpr prn)

  (-> (gateless-rules/analyze-defun-macro
        {:node (api/parse-string "(defun foo-bar :foo/bar [foo] foo)")}) :node api/sexpr prn)
  (-> (gateless-rules/analyze-defdata-macro
        {:node (api/parse-string "(defdata loan-batch :loaders/batch \"foobar\")")}) :node api/sexpr prn)

  (-> (gateless-rules/analyze-defun-macro
        {:node (api/parse-string "(defun update-map [m f]
                                    (reduce-kv (fn [m k v]
                                                 (assoc m k (f v))) {} m))")}) :node api/sexpr prn)
  (-> (clara-rules/analyze-defrule-macro
        {:node (api/parse-string "(defrule unmatch-borrower-credit-report-context-singleton-rule
                                    \"there can be only one\"
                                    [?context-seq <- (acc/all) :from [:context/borrower-credit-report-no-match]]
                                    [:test (> (count ?context-seq) 1)]
                                    =>
                                    (run! retract! ?context-seq))")}) :node api/sexpr prn)

  (-> (clara-rules/analyze-defrule-macro
        {:node (api/parse-string "(defrule ^:RUL-1812 doc-expiration-verification-rule-paystub
                                    \"This rule verifies whether the given Paystub document is expired as per the guidelines set\"
                                    [:test (<= (jt/time-between a b :days))]
                                    =>
                                    (insert! (->fact :context/valid-paystub-document
                                                     {:header-id 123})))")})
      :node api/sexpr prn)
  )
