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
  )
