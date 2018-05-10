(ns side-fx.test-runner
  (:require
   [clojure.test :refer [run-tests]]
   [side-fx.fetch-test]))

(defn run-all-tests []
  (run-tests
    'side-fx.fetch-test))

(defn -main [& args]
  (run-all-tests))

(set! *main-cli-fn* -main)
