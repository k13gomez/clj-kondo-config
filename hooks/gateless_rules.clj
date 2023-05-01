(ns hooks.gateless-rules
  (:require
    [clj-kondo.hooks-api :as api]))

(defn analyze-defun-macro
  "analyze gateless rules defun macro"
  [{:keys [:node]}]
  (let [[var-name & children] (rest (:children node))
        [var-fact & children] (if (and (= :token (api/tag (first children)))
                                       (keyword? (api/sexpr (first children)))
                                       (seq (rest children)))
                                children
                                (cons nil children))
        [var-docs var-args & body] (if (= :token (api/tag (first children)))
                                     children
                                     (cons nil children))
        new-meta (cond-> {}
                   var-fact (assoc :clj-kondo/ignore [:clojure-lsp/unused-public-var]))
        new-node (with-meta
                   (api/list-node
                     (cond-> (list (api/token-node 'defn) var-name)
                       var-docs (concat [var-docs])
                       :always (concat [var-args] body)))
                   new-meta)]
    {:node new-node}))

(defn analyze-defdata-macro
  "analyze gateless rules defdata macro"
  [{:keys [:node]}]
  (let [[var-name & children] (rest (:children node))
        [var-fact & body] (if (and (= :token (api/tag (first children)))
                                   (keyword? (api/sexpr (first children)))
                                   (seq (rest children)))
                            children
                            (cons nil children))
        new-meta (cond-> {}
                   var-fact (assoc :clj-kondo/ignore [:clojure-lsp/unused-public-var]))
        new-node (with-meta
                   (api/list-node
                     (list*
                       (api/token-node 'def)
                       var-name
                       body))
                   new-meta)]
    {:node new-node}))
