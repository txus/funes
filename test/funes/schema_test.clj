(ns funes.schema-test
  (:require [funes.schema :as sut :refer [generate-schema generalize-values]]
            [funes.ast :refer [->AST]]
            [schema.core :as s]
            [clojure.test :as t :refer :all]))

(deftest generate-schema-test
  (testing "generates schemas from ASTs"
    (are [schema ast] (= schema (generate-schema ast))
      `(partial = 3)
      (->AST :value 3)

      `(s/maybe (partial = 3))
      (->AST :maybe (->AST :value 3))

      `s/Any
      (->AST :any nil)

      'nil
      (->AST :nil nil)

      `(s/optional-key :foo)
      (->AST :optional-key :foo)

      `(s/enum :foo :bar)
      (->AST :enum [:foo :bar])

      `s/Bool
      (->AST :type java.lang.Boolean)

      `s/Num
      (->AST :type java.lang.Number)

      `s/Str
      (->AST :type java.lang.String)

      `s/Inst
      (->AST :type java.util.Date)

      `s/Keyword
      (->AST :type clojure.lang.Keyword)

      `[s/Num]
      (->AST :seq (->AST :type java.lang.Number))

      `{(s/optional-key :foo) s/Num}
      (->AST :map {(->AST :optional-key :foo)
                   (->AST :type java.lang.Number)}))))

(deftest generalize-values-test
  (testing "generalizes values for more flexible schemas"
    (testing "generalizing types"
      (are [post pre] (= post (generalize-values pre))
        (->AST :type java.lang.Number)
        (->AST :type java.lang.Integer)

        (->AST :type java.lang.Number)
        (->AST :type java.lang.Double)

        (->AST :type java.lang.Number)
        (->AST :type java.lang.Long)))

    (testing "generalizing numbers"
      (is (= (->AST :type java.lang.Number)
             (generalize-values (->AST :value 3)))))

    (testing "generalizing strings"
      (is (= (->AST :type java.lang.String)
             (generalize-values (->AST :value "hey")))))

    (testing "generalizing keywords"
      (is (= (->AST :type clojure.lang.Keyword)
             (generalize-values (->AST :value :foo)))))

    (testing "generalizing any other values"
      (is (= (->AST :type java.util.Date)
             (generalize-values (->AST :value #inst "2016-01-01")))))))
