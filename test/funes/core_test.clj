(ns funes.core-test
  (:require  [clojure.test :refer :all]
             [schema.core :as s]
             [funes.core :as sut :refer [infer lift ->AST]]))

(deftest lift-test
  (testing "lifts any value to an AST"
    (are [x] (lift x)
      (->AST :any nil)
      (->AST :any nil)

      (->AST :map {:foo (->AST :value 3)
                   :bar (->AST :nil nil)})
      {:foo 3
       :bar nil}

      (->AST :nil nil)
      nil

      (->AST :type java.lang.Number)
      java.lang.Number

      (->AST :seq (->AST :any nil))
      []

      (->AST :seq (->AST :type java.lang.Number))
      [1 2 3]

      (->AST :value 3)
      3)))

(deftest infer-test
  (testing "infers a if a and b are equal"
    (is (= (->AST :value 3)
           (infer (->AST :value 3)
                  (->AST :value 3)))))
  (testing "when both are types"
    (testing "infers the supertype if there is a subtyping relation"
      (is (= (->AST :type java.lang.Number)
             (infer (->AST :type java.lang.Integer)
                    (->AST :type java.lang.Number))))
      (is (= (->AST :type java.lang.Number)
             (infer (->AST :type java.lang.Number)
                    (->AST :type java.lang.Integer)))))
    (testing "finds the common superclass if there is one (but not java.lang.Object)"
      (is (= (->AST :type java.lang.Number)
             (infer (->AST :type java.lang.Number)
                    (->AST :type java.lang.Double)))))
    (testing "defaults to any if the only superclass is java.lang.Object"
      (is (= (->AST :any nil)
             (infer (->AST :type java.lang.Number)
                    (->AST :type java.lang.String))))))
  (testing "when at least one of them is a type lifts the other one as a type and tries again"
    (is (= (->AST :type java.lang.Number)
           (infer (->AST :value 3)
                  (->AST :type java.lang.Double))))
    (is (= (->AST :type java.lang.Number)
           (infer (->AST :type java.lang.Number)
                  (->AST :value 3)))))
  (testing "when one of them is nil treats the other one as a maybe"
    (is (= (->AST :maybe (->AST :value 3))
           (infer (->AST :value 3)
                  (->AST :nil nil))))
    (is (= (->AST :maybe (->AST :type java.lang.Number))
           (infer (->AST :nil nil)
                  (->AST :type java.lang.Number)))))
  (testing "with maps"
    (testing "unifies keys"
      (is (= (->AST :map {:foo (->AST :type java.lang.Long)
                          :bar (->AST :nil nil)})
             (infer (->AST :map {:foo (->AST :value 3)
                                 :bar (->AST :nil nil)})
                    (->AST :map {:foo (->AST :value 9)
                                 :bar (->AST :nil nil)})))))
    (testing "detects optional keys"
      (is (= (->AST :map {(->AST :optional-key :foo) (->AST :value 9)})
             (infer (->AST :map {})
                    (->AST :map {:foo (->AST :value 9)}))))
      (is (= (->AST :map {(->AST :optional-key :foo) (->AST :value 9)})
             (infer (->AST :map {:foo (->AST :value 9)})
                    (->AST :map {}))))))
  (testing "with keywords it unifies them as enums"
    (is (= (->AST :enum #{:foo :bar})
           (infer (->AST :value :foo)
                  (->AST :value :bar))))
    (is (= (->AST :enum #{:foo :bar :baz})
           (infer (->AST :enum #{:bar :baz})
                  (->AST :value :foo))))
    (is (= (->AST :enum #{:foo :bar :baz})
           (infer (->AST :value :foo)
                  (->AST :enum #{:bar :baz})))))
  (testing "with other values it generalizes them"
    (is (= (->AST :type java.lang.Long)
           (infer (->AST :value 3)
                  (->AST :value 0)))))
  (testing "when it cannot unify it defaults to any"
    (is (= (->AST :any nil)
           (infer (->AST :value 3)
                  (->AST :value "hey")))))
  (testing "inferring non-ast values"
    (is (= (->AST :seq (->AST :type java.lang.Long))
           (infer [1] [2])))
    (is (= (->AST :seq (->AST :type java.lang.Long))
           (infer [1] (->AST :seq (->AST :type java.lang.Long)))))
    (is (= (->AST :map {})
           (infer {} {})))
    (is (= (->AST :enum #{:foo :bar})
           (infer :foo :bar)))
    (is (= (->AST :type java.lang.Long)
           (infer 3 9)))))
