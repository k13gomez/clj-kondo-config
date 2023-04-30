(defrule foo-rule
  "this rule inserts stuff"
  {:salience 100}
  [?fact <- :foo/bar [{:keys [foo-value]}]
   (= foo-value ?foo-value)
   (some? ?fact)] ;;; should return unresolved symbol
  [:foo/fact]
  [?foo-data <- :foo/data]
  [:not [:foo/not [{:keys [foo-not]}] (= foo-not ?foo-value)]]
  [:and
   [:foo/thing1]
   [:not [:foo/begone [{:keys [foo-not]}]
          (= foo-not ?foo-not)
          (true? ?foo-not)
          (true? foo-yes)]] ;;; should return unresolved-symbol
   [:foo/thing2 [{:keys [foo-thing1]}] (= foo-thing1 ?foo-value)]
   [:or
    [:foo/maybe1]
    [:foo/maybe2 [{:keys [foo-maybe]}] (= foo-maybe ?foo-value)]]]
  [:exists
   [:foo/required1 [{:keys [foo-required]}] (true? foo-required)]]
  [:exists
   [:foo/required]]
  [?acc-result1 <- (acc/grouping-by :foo-hash) :from [:foo/item1 [{:keys [foo-item]}] (= foo-item ?foo-value)]]
  [?acc-result2 <- (acc/all) :from [:foo/item2]]
  [?acc-result3 <- (acc/all) :from [:foo/item3 (= foo-item ?foo-item)]] ;;; should return unresolved-symbol
  [:test (and (some? ?foo-value)
              (some? ?fact)
              (seq ?acc-result1)
              (seq ?acc-result2))]
  =>
  (insert! {:fact/type :foo/result
            :other foo-value ;;; should return unresolved-symbol
            :fact ?fact
            :result ?foo-value}))
