(ns hooks.clara-rules
  (:require [clj-kondo.hooks-api :as api]
            [clojure.string :as str]
            [clojure.set :as set]))

(defn node-value
  [node]
  (when node
    (api/sexpr node)))

(defn node-type
  [node]
  (when node
    (api/tag node)))

(defn- binding-node?
  "determine if a symbol is a clara-rules binding symbol in the form `?<name>`"
  [node]
  (let [node-name (node-value node)]
    (and (symbol? node-name)
         (str/starts-with? (name node-name) "?"))))

(defn extract-arg-tokens
  [node-seq]
  (reduce (fn [token-seq node]
            (cond
              (and (= :token (node-type node))
                   (symbol? (node-value node))
                   (not (binding-node? node))
                   (nil? (resolve (node-value node))))
              (cons node token-seq)

              (seq (:children node))
              (concat token-seq (extract-arg-tokens (:children node)))

              :else token-seq)) [] node-seq))

(defn analyze-constraints
  "sequentially analyzes constraint expressions of clara rules and queries
  defined via defrule or defquery by sequentially analyzing its children lhs
  expressions and bindings."
  [condition prev-bindings input-token production-args]
  (let [[condition-args constraint-seq]
        (if (= :vector (node-type (first condition)))
          [(first condition) (rest condition)]
          [(api/vector-node (vec (extract-arg-tokens condition))) condition])
        args-binding-set (set (map node-value (:children production-args)))
        prev-bindings-set (->> (mapcat (comp :children first) prev-bindings)
                               (filter binding-node?)
                               (map node-value)
                               (set))
        constraint-bindings
        (loop [[constraint-expr & more] constraint-seq
               bindings []
               bindings-set (set/union prev-bindings-set args-binding-set)]
          (if (nil? constraint-expr)
            bindings
            (let [constraint (:children constraint-expr)
                  binding-nodes (when (= '= (node-value (first constraint)))
                                  (seq (filter binding-node? (rest constraint))))
                  next-bindings-set (-> (set (map node-value binding-nodes))
                                        (set/difference bindings-set))
                  binding-expr-nodes (seq (filter (comp next-bindings-set node-value) binding-nodes))
                  [next-bindings-set next-bindings]
                  (if binding-nodes
                    [next-bindings-set
                     (cond->> [[(api/vector-node
                                  (vec binding-nodes))
                                constraint-expr]]
                       binding-expr-nodes
                       (concat [[(api/vector-node
                                   (vec binding-expr-nodes))
                                 input-token]]))]
                    [#{}
                     [[(api/vector-node
                         [(api/token-node '_)])
                       constraint-expr]]])]
              (recur more
                     (concat bindings next-bindings)
                     (set/union bindings-set next-bindings-set)))))

        input-bindings (when-not (empty? (node-value condition-args))
                         [[condition-args input-token]])]
    (concat input-bindings constraint-bindings)))

(defn analyze-conditions
  "sequentially analyzes condition expressions of clara rules and queries
  defined via defrule and defquery by taking into account the optional
  result binding, optional args bindings and sequentially analyzing
  its children constraint expressions."
  [condition-seq prev-bindings input-token production-args]
  (loop [[condition-expr & more] condition-seq
         bindings []]
    (if (nil? condition-expr)
      bindings
      (let [condition (:children condition-expr)
            [result-token fact-node & condition] (if (= '<- (-> condition second node-value))
                                                   (cons (api/vector-node
                                                           [(first condition)]) (nnext condition))
                                                   (cons (api/vector-node
                                                           [(api/token-node '_)]) condition))
            condition-bindings (cond
                                 (nil? condition)
                                 []

                                 (contains? #{:not :or :and :exists} (node-value fact-node))
                                 (analyze-conditions condition (concat prev-bindings bindings) input-token production-args)

                                 (and (= :list (node-type fact-node))
                                      (= :from (-> condition first node-value)))
                                 (analyze-conditions (rest condition) (concat prev-bindings bindings) input-token production-args)

                                 :else
                                 (analyze-constraints condition (concat prev-bindings bindings) input-token production-args))
            condition-tokens (->> (mapcat first condition-bindings)
                                  (filter binding-node?))
            result-vector (api/vector-node (vec (list* fact-node condition-tokens)))
            result-bindings [[result-token result-vector]]
            output-bindings (concat condition-bindings result-bindings)
            condition-output (->> (mapcat (comp :children first) output-bindings)
                                  (filter binding-node?)
                                  (set)
                                  (sort-by node-value))
            output-node (api/vector-node
                          (if (empty? condition-output)
                            [(api/token-node '_)]
                            (vec condition-output)))
            output-result-node (api/vector-node
                                 (if (empty? condition-output)
                                   [(api/token-node nil)]
                                   (vec condition-output)))
            next-bindings [output-node
                           (api/list-node
                             (list
                               (api/token-node 'let)
                               (api/vector-node
                                 (vec (apply concat output-bindings)))
                               output-result-node))]]
        (recur more (concat bindings [next-bindings]))))))

(defn analyze-defquery-macro
  "analyze clara-rules defquery macro"
  [{:keys [:node]}]
  (let [[production-name & children] (rest (:children node))
        production-docs (when (= :token (node-type (first children)))
                          (first children))
        children (if production-docs (rest children) children)
        production-opts (when (= :map (node-type (first children)))
                          (first children))
        input-token (api/token-node (gensym 'input))
        input-args (api/vector-node
                     [input-token])
        [production-args & condition-seq] (if production-opts (rest children) children)
        condition-bindings (analyze-conditions condition-seq [] input-token production-args)
        production-bindings (apply concat [production-args input-token] condition-bindings)
        production-output (->> (mapcat (comp :children first) condition-bindings)
                               (filter binding-node?)
                               (set)
                               (sort-by node-value))
        production-result (api/list-node
                            (list
                              (api/token-node 'let)
                              (api/vector-node
                                (vec production-bindings))
                              (api/vector-node
                                (vec production-output))))
        new-node (api/list-node
                   (cond-> (list (api/token-node 'defn) production-name)
                     production-docs (concat [production-docs])
                     :always (concat [input-args])
                     production-opts (concat [production-opts])
                     :always (concat [production-result])))]
    {:node new-node}))

(defn analyze-defrule-macro
  "analyze clara-rules defrule macro"
  [{:keys [:node]}]
  (let [[production-name & children] (rest (:children node))
        production-docs (when (= :token (node-type (first children)))
                          (first children))
        children (if production-docs (rest children) children)
        production-opts (when (= :map (node-type (first children)))
                          (first children))
        input-token (api/token-node (gensym 'input))
        input-args (api/vector-node
                     [input-token])
        empty-args (api/vector-node [])
        production-seq (if production-opts (rest children) children)
        [condition-seq _ body-seq] (partition-by (comp #{'=>} node-value) production-seq)
        condition-bindings (analyze-conditions condition-seq [] input-token empty-args)
        production-bindings (apply concat [] condition-bindings)
        production-output (->> (mapcat (comp :children first) condition-bindings)
                               (filter binding-node?)
                               (set)
                               (sort-by node-value))
        production-result (api/list-node
                            (list*
                              (api/token-node 'let)
                              (api/vector-node
                                (vec production-bindings))
                              (api/vector-node
                                (vec production-output))
                              body-seq))
        new-node (api/list-node
                   (cond-> (list (api/token-node 'defn) production-name)
                     production-docs (concat [production-docs])
                     :always (concat [input-args])
                     production-opts (concat [production-opts])
                     :always (concat [production-result])))]
    {:node new-node}))
