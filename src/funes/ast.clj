(ns funes.ast)

(defrecord AST [tag inferrable])

(defn ast? [x]
  (instance? AST x))
