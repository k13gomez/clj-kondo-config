(ns hooks.gateless
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
        new-node (vary-meta
                   (api/list-node
                     (cond-> (list (api/token-node 'clojure.core/defn) var-name)
                       var-docs (concat [var-docs])
                       :always (concat [var-args] body)))
                   merge new-meta)]
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
        new-node (vary-meta
                   (api/list-node
                     (list*
                       (api/token-node 'clojure.core/def)
                       var-name
                       body))
                   merge new-meta)]
    {:node new-node}))

(defn analyze-overlay-updates-macro
  [{:keys [:node]}]
  (let [children (rest (:children node))
        new-node (api/list-node
                   (list*
                     (api/token-node 'clojure.core/->)
                     (api/token-node 'nil)
                     children))]
    {:node new-node}))

(defn analyze-with-aws-xray-macro
  [{:keys [:node]}]
  (let [children (rest (:children node))
        [segment-name & body-seq] (if (= :map (node-type (first children)))
                                    (rest children)
                                    children)
        new-node (api/list-node
                   (list*
                     (api/token-node 'let)
                     (api/vector-node
                       [segment-name (api/keyword-node :segment)])
                     body-seq))]
    {:node new-node}))

(defmacro bind-test-ns-and-rule-sources
  [& _args]
  `(do
     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'compile-rules!
       []
       :done)

     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'run-rules-and-query-map!
       [~'facts ~'query-map & {:as ~'options}]
       [:done ~'facts ~'query-map ~'options])

     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'run-rules-and-query!
       [~'facts ~'query]
       (-> (~'run-rules-and-query-map! ~'facts {[:output :one :output] ~'query})
           (:output)))

     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'run-rules-and-query-one!
       [~'facts ~'query ~'k]
       (-> (~'run-rules-and-query-map! ~'facts {[:output :one ~'k] ~'query})
           (:output)))

     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'run-rules-and-query-all!
       [~'facts ~'query ~'k]
       (-> (~'run-rules-and-query-map! ~'facts {[:output :all ~'k] ~'query})
           (:output)))

     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'run-rules-and-query-sorted!
       [~'facts ~'query ~'s ~'k]
       (-> (~'run-rules-and-query-map!
             ~'facts
             {[:output (partial sort-by ~'s) ~'k] ~'query})
           (:output)))

     (defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
       ~'run-rules-and-query-set!
       [~'facts ~'query ~'k]
       (-> (~'run-rules-and-query-map! ~'facts {[:output :set ~'k] ~'query})
           (:output)))

     [(var ~'compile-rules!)
      (var ~'run-rules-and-query-map!)
      (var ~'run-rules-and-query!)
      (var ~'run-rules-and-query-one!)
      (var ~'run-rules-and-query-all!)
      (var ~'run-rules-and-query-sorted!)
      (var ~'run-rules-and-query-set!)]))
