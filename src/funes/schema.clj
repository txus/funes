(ns funes.schema
  "Generates Schemas from Funes ASTs"
  (:require
   [clojure.walk :as w]
   [schema.core :as s])
  (:import [funes.core AST]))

(defmulti generate-schema :tag)

(defmethod generate-schema :value
  [{:keys [inferrable]}]
  `(partial = ~(generate-schema inferrable)))

(defmethod generate-schema :maybe
  [{:keys [inferrable]}]
  `(s/maybe ~(generate-schema inferrable)))

(defmethod generate-schema :any
  [{:keys [_]}]
  `s/Any)

(defmethod generate-schema :nil
  [{:keys [_]}]
  'nil)

(defmethod generate-schema :optional-key
  [{:keys [inferrable]}]
  `(s/optional-key ~inferrable))

(defmethod generate-schema :enum
  [{:keys [inferrable]}]
  `(s/enum ~@inferrable))

(defmethod generate-schema :type
  [{:keys [inferrable]}]
  (condp = inferrable
    java.lang.Boolean
    `s/Bool

    java.lang.Number
    `s/Num

    java.lang.String
    `s/Str

    java.util.Date
    `s/Inst

    clojure.lang.Keyword
    `s/Keyword

    inferrable))

(defmethod generate-schema :seq
  [{:keys [inferrable]}]
  `[~(generate-schema inferrable)])

(defmethod generate-schema :map
  [{:keys [inferrable]}]
  (into {} (map (fn [[k v]]
                  [(generate-schema k)
                   (generate-schema v)])
                inferrable)))

(defmethod generate-schema :default
  [v]
  v)

(defn generalize-type [t]
  (get {java.lang.Integer java.lang.Number
        java.lang.Double  java.lang.Number
        java.lang.Long    java.lang.Number} t t))

(defn generalize-values
  "Transforms an AST generalizing number types and values to their types, in
  order to be able to generate more idiomatic Schema schemas."
  [ast]
  (w/postwalk
    (fn [node]
      (if (instance? AST node)
        (let [{:keys [tag inferrable]} node]
          (cond (= tag :type)
                (AST. :type (generalize-type inferrable))

                (= tag :optional-key)
                (AST. :optional-key (generalize-type inferrable))

                (number? inferrable)
                (AST. :type java.lang.Number)

                (string? inferrable)
                (AST. :type java.lang.String)

                (keyword? inferrable)
                (AST. :type clojure.lang.Keyword)

                (= :value tag)
                (AST. :type (generalize-type (type inferrable)))

                :else
                node))
        node))
    ast))
