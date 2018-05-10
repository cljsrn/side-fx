(ns side-fx.fetch-test
  (:require
   [clojure.test :refer [deftest is]]
   [side-fx.fetch :as fetch]))

(deftest form-method-test
  (is (= "GET" (#'fetch/form-method {::fetch/method :get}))))
