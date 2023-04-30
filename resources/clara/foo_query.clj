(defquery ^:RUL-123 foo-query
  "this query returns stuff"
  {:salience 100}
  [?foo-query-value]
  [?query-output <- :foo/bar [{:keys [foo-query-value
                                      bar-query-value]}]
   (= foo-query-value ?foo-query-value)
   (= bar-query-value ?bar-query-value)
   (some? ?foo-query-value)]
  [:test (some? ?query-output)])
