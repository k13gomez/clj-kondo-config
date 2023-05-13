(ns user
  (:require [clj-kondo.hooks-api :as api]
            [clojure.string :as str]
            [hooks.clara-rules :as clara-rules]
            [hooks.gateless :as gateless]))

(def query-node
  (api/parse-string (slurp "resources/clara/foo_query.clj")))

(def rule-node
  (api/parse-string (slurp "resources/clara/foo_rule.clj")))

(def utils-node
  (api/parse-string (slurp "resources/clara/test_utils.clj")))

;; scratch
(comment
  (declare insert! ->fact)

  (-> (clara-rules/analyze-def-rules-test-macro {:node utils-node}) :node api/sexpr)

  (-> (clara-rules/analyze-defquery-macro {:node query-node}) :node api/sexpr prn)
  (-> (clara-rules/analyze-defrule-macro {:node rule-node}) :node api/sexpr prn)

  (-> (gateless/analyze-defun-macro
        {:node (api/parse-string "(defun foo-bar :foo/bar [foo] foo)")}) :node api/sexpr prn)
  (-> (gateless/analyze-defdata-macro
        {:node (api/parse-string "(defdata loan-batch :loaders/batch \"foobar\")")}) :node api/sexpr prn)

  (-> (gateless/analyze-defun-macro
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
                                    [?context <- :test/context]
                                    [?foobar <- :foo/bar [{:keys [foo]}] (= foo (:foo ?context))]
                                    [?barfoo <- :bar/foo]
                                    =>
                                    (insert! (->fact :context/foobar {:value ?barfoo})))")})
      :node api/sexpr)

  (-> (clara-rules/analyze-defrule-macro
        {:node (api/parse-string "(defrule doc-expiration-verification-rule-paystub
                                    \"comments\"
                                    =>
                                    (insert! (->fact :context/foobar {:value ?barfoo})))")})
      some?)

  (-> (clara-rules/analyze-parse-query-macro
        {:node (api/parse-string "(clara.rules.dsl/parse-query [] [[:not [Second]]
                                                                   [:not [Third]]
                                                                   [?fourth <- Fourth]])")})
      :node api/sexpr prn)

  (-> (clara-rules/analyze-parse-rule-macro
        {:node (api/parse-string "(dsl/parse-rule
                                    [[::cold]]
                                    (insert! (->fact ::hot nil)))")})
      :node api/sexpr prn)

  (-> (clara-rules/analyze-defquery-macro
        {:node (api/parse-string "(defquery cold-query
                                    [:?l]
                                    [Temperature (< temperature 50)
                                     (= ?t temperature)
                                     (= ?l location)])")})
      :node api/sexpr prn))
  
