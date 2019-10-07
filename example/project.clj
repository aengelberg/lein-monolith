(defproject example/all "MONOLITH"
  :description "Overarching example project."

  :plugins
  [[lein-monolith "1.3.0"]
   [lein-pprint "1.2.0"]]

  :dependencies
  [[org.clojure/clojure "1.9.0"]]

  :test-selectors
  {:unit (complement :integration)
   :integration :integration}

  :monolith
  {:inherit
   [:test-selectors
    :env]

   :inherit-leaky
   [:repositories
    :managed-dependencies]

   :project-selectors
   {:deployable :deployable
    :unstable #(= (first (:version %)) \0)}

   :project-dirs
   ["apps/app-a"
    "libs/*"
    "not-found"]}

  :env
  {:foo "bar"})
