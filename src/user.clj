(ns user
  (:require [clj-kondo.hooks-api :as api]
            [hooks.clara-rules :as clara-rules]))

(def query-node
  (api/parse-string (slurp "resources/clara/foo_query.clj")))

(def rule-node
  (api/parse-string (slurp "resources/clara/foo_rule.clj")))

;; scratch
(comment
  (-> (clara-rules/analyze-defquery-macro {:node query-node}) :node api/sexpr prn)
  (-> (clara-rules/analyze-defrule-macro {:node rule-node}) :node api/sexpr prn)
  )
