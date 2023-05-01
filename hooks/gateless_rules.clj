(ns hooks.gateless-rules
  (:require
    [clj-kondo.hooks-api :as api]))

(defn node-value
  [node]
  (when node
    (api/sexpr node)))

(defn node-type
  [node]
  (when node
    (api/tag node)))

(defn analyze-defun-macro
  "analyze gateless rules defun macro"
  [{:keys [:node]}]
  (let [[var-name & children] (rest (:children node))
        [var-fact & children] (if (and (= :token (node-type (first children)))
                                       (keyword? (node-value (first children)))
                                       (seq (rest children)))
                                children
                                (cons nil children))
        [var-docs var-args & body] (if (= :token (node-type (first children)))
                                     children
                                     (cons nil children))
        new-meta (cond-> {}
                   var-fact (assoc :clj-kondo/ignore [:clojure-lsp/unused-public-var]))
        new-node (with-meta
                   (api/list-node
                     (cond-> (list (api/token-node 'clojure.core/defn) var-name)
                       var-docs (concat [var-docs])
                       :always (concat [var-args] body)))
                   new-meta)]
    {:node new-node}))

(defn analyze-defdata-macro
  "analyze gateless rules defdata macro"
  [{:keys [:node]}]
  (let [[var-name & children] (rest (:children node))
        [var-fact & body] (if (and (= :token (node-type (first children)))
                                   (keyword? (node-value (first children)))
                                   (seq (rest children)))
                            children
                            (cons nil children))
        new-meta (cond-> {}
                   var-fact (assoc :clj-kondo/ignore [:clojure-lsp/unused-public-var]))
        new-node (with-meta
                   (api/list-node
                     (list*
                       (api/token-node 'clojure.core/def)
                       var-name
                       body))
                   new-meta)]
    {:node new-node}))
