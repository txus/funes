(def project 'funes)
(def version "0.1.0")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "RELEASE"]
                            [prismatic/schema "1.1.6"]
                            [adzerk/boot-test "RELEASE" :scope "test"]])

(task-options!
 pom {:project     project
      :version     version
      :description "Infer the shape of data and optionally produce a schema from it"
      :url         "https://blog.txus.io/funes"
      :scm         {:url "https://github.com/txus/funes"}
      :license     {"The MIT License (MIT)"
                    "http://opensource.org/licenses/mit-license.php"}})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

(deftask release
  []
  (comp (pom) (jar) (push)))

(require '[adzerk.boot-test :refer [test]])
