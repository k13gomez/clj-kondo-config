(tu/def-rules-test test-nodes-have-named-fns
  {:rules [;; covers AlphaNode, ExpressionJoinNode and ProductionNode
           expression-node-rule [[[::a [{:keys [some-field]}] (= ?some-field some-field)]
                                  [::b [{:keys [another-field]}] (contains? another-field ?some-field)]]
                                 (r/insert! {:fact-type ::c})]
           ;; covers AccumulateNode, TestNode
           accum-test-rule [[[?cs <- (acc/all) :from [::c]]
                             [:test (seq ?cs)]]
                            (r/insert! {:fact-type ::d :vals ?cs})]
           ;; covers AccumulateWithJoinFilterNode
           accum-join-filter [[[::a [{:keys [some-field]}] (= ?some-field some-field)]
                               [?ds <- (acc/all) :from [::d [{:keys [another-field]}] (contains? another-field ?some-field)]]]
                              (r/insert! {:fact-type ::e :vals ?ds})]
           ;; covers NegationWithJoinFilter
           negation-join-filter [[[::e [{:keys [some-field]}] (= ?some-field some-field)]
                                  [:not [::d [{:keys [another-field]}] (contains? another-field ?some-field)]]]
                                 (r/insert! {:fact-type ::f})]]
   :queries []
   :sessions [base-session [expression-node-rule
                            accum-test-rule
                            accum-join-filter
                            negation-join-filter] {:fact-type-fn :fact-type}]}
  (let [get-node-fns (fn [node]
                       (condp instance? node
                         AlphaNode [(:activation node)]
                         TestNode [(-> node :test :handler)]
                         AccumulateNode []
                         AccumulateWithJoinFilterNode [(:join-filter-fn node)]
                         ProductionNode [(:rhs node)]
                         NegationWithJoinFilterNode [(:join-filter-fn node)]
                         ExpressionJoinNode [(:join-filter-fn node)]
                         RootJoinNode []))]
    (doseq [node (-> base-session eng/components :rulebase :id-to-node vals)
            node-fn (get-node-fns node)]
      (is (seq (re-find (re-pattern (str (get eng/node-type->abbreviated-type (.getSimpleName (class node)))
                                         "-"
                                         (:id node)))
                        (-> node-fn str m/demunge (str/split #"/") last)))
          (str "For node: " node " and node-fn: " node-fn)))))
