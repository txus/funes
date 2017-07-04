(ns funes.core
  "Infers the shape of some data from at least two examples.

  `infer` takes any two inferrables (plain values or AST nodes) and returns an
  AST node representing the inferred shape of both.

  AST nodes form monoids with `infer`, such that you can fold a collection of
  inferrables to obtain the most general shape describing all of them.

  `generate-schema` takes an AST node and spits a syntax-quoted data structure
  representing a `schema.core` schema."
  (:require [clojure.set :as set]
            [funes.schema :as s]
            [funes.ast :refer [ast? ->AST]]
            [clojure.walk :as w])
  (:import [funes.ast AST]))

(defprotocol Inferrable
  (infer [a b]))

(defn- find-closest-common-superclass [^Class a ^Class b]
  (loop [kls a]
    (if (not (.isAssignableFrom kls b))
      (recur (.getSuperclass kls))
      kls)))

(declare lift)

(extend-type AST
  Inferrable
  (infer [a b]
    (let [b (lift b)
          tags [(:tag a) (:tag b)]]
      (cond
        (= a b)
       a

        (= [:type :type] tags)
        (let [type-a (:inferrable a)
              type-b (:inferrable b)]
          (cond (isa? type-a type-b)
                b

                (isa? type-b type-a)
                a

                :else
                (let [kls (find-closest-common-superclass type-a type-b)]
                  (if (= kls java.lang.Object)
                    (AST. :any nil)
                    (AST. :type kls)))))

        (= [:type :value] tags)
        (infer a (lift (type (:inferrable b))))

        (= [:value :type] tags)
        (infer b (lift (type (:inferrable a))))

        (= [:seq :seq] tags)
        (AST. :seq (infer (:inferrable a) (:inferrable b)))

        (and (= [:value :value] tags)
             (keyword? (:inferrable a))
             (keyword? (:inferrable b)))
        (AST. :enum #{(:inferrable a)
                      (:inferrable b)})

        (= :nil (first tags))
        (cond
          (= :maybe (last tags))
          b
          :else
          (AST. :maybe b))

        (= :nil (last tags))
        (cond
          (= :maybe (first tags))
          a
          :else
          (AST. :maybe a))

        (and (= :enum (first tags))
             (= :value (last tags))
             (keyword? (:inferrable b)))
        (AST. :enum (conj (:inferrable a) (:inferrable b)))

        (and (= :value (first tags))
             (= :enum (last tags))
             (keyword? (:inferrable a)))
        (AST. :enum (conj (:inferrable b) (:inferrable a)))

        (= [:map :map] tags)
        (let [a' (:inferrable a)
              b' (:inferrable b)
              a-keys (set (keys a'))
              b-keys (set (keys b'))
              keys-not-in-b (set/difference a-keys b-keys)
              keys-not-in-a (set/difference b-keys a-keys)
              common-keys (set/intersection a-keys b-keys)]
          (AST. :map (merge-with infer
                                 (select-keys a' common-keys)
                                 (select-keys b' common-keys)
                                 (into {} (map (fn [[k v]]
                                                 [(if (ast? k)
                                                    k
                                                    (AST. :optional-key k))
                                                  (lift v)])
                                               (select-keys a' keys-not-in-b)))
                                 (into {} (map (fn [[k v]]
                                                 [(if (ast? k)
                                                    k
                                                    (AST. :optional-key k))
                                                  (lift v)])
                                               (select-keys b' keys-not-in-a))))))

        (and (apply = tags)
             (= (type (:inferrable a)) (type (:inferrable b))))
        (AST. :type (type (:inferrable a)))

        :else
        (AST. :any nil)))))

(defn lift
  "Lifts any value to an AST representation."
  [v]
  (cond
    (ast? v)
    v

    (map? v)
    (AST. :map (->> v (map (fn [[k v]] [k (lift v)])) (into {})))

    (nil? v)
    (AST. :nil nil)

    (class? v)
    (AST. :type v)

    (sequential? v)
    (cond
      (empty? v)
      (AST. :seq (AST. :any nil))

      (= 1 (count v))
      (AST. :seq (lift (first v)))

      :else
      (AST. :seq (reduce infer v)))

    :else
    (AST. :value v)))

(extend-type nil
  Inferrable
  (infer [_ b]
    (if (nil? b)
      (AST. :nil nil)
      (AST. :maybe (lift b)))))

(extend-type Object
  Inferrable
  (infer [a b]
    (cond
      (= a b)
      (lift a)

      (or
       (sequential? a)
       (map? a)
       (keyword? a))
      (infer (lift a) (lift b))

      (= (type a) (type b))
      (AST. :type (type a))

      :else
      (AST. :any nil))))

(defn- range*
  "Returns a set of all values obtained by mapping f over xs, or a numeric range
  as a pair of smallest and largest number, if it's numeric."
  ([xs] (range* identity xs))
  ([f xs]
   (let [ys (map f xs)]
     (if (every? number? ys)
       (let [sorted (sort ys)]
         [(first sorted) (last sorted)])
       (into #{} ys)))))

(defn overview
  "Takes a collection of maps and returns a map that describes all of them.
   For each key, the value will be either:

     a) a specific value if it's the same in all maps, or
     b) a pair of smallest and largest number if it's a numeric field, or
     c) a set of all different values, as long as it's smaller than `limit`, or
     d) a Schema otherwise"
  [xs limit]
  (->> (into #{} (mapcat keys xs))
       (map (fn [k]
              (let [ks (range* k xs)]
                (cond
                  (> (count ks) limit)
                  (let [schema (->> ks
                                    (reduce infer)
                                    s/generalize-values
                                    s/generate-schema)]
                    [k schema])
                  (= 1 (count ks))
                  [k (first ks)]

                  :else
                  [k ks]))))
       (into {})))

(defn ->schema [xs]
  (->> xs
       (reduce infer)
       s/generalize-values
       s/generate-schema))
