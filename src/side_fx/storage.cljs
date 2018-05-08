(ns side-fx.storage
  (:require
   [cljs.reader :refer [read-string]]
   [re-frame.core :refer [reg-fx dispatch]]))

(def ^:private async-storage (.-AsyncStorage (js/require "react-native")))

(defn set-item
  [{:keys [key value on-success on-failure]
    :or   {on-success [::no-on-success]
           on-failure [::no-on-failure]}}]
  (.setItem async-storage (pr-str key) (pr-str value)
    (fn [error]
      (if-not error
        (dispatch on-success)
        (dispatch (conj on-failure error))))))

(defn get-item
  [{:keys [key on-success on-failure]
    :or   {on-success [::no-on-success]
           on-failure [::no-on-failure]}}]
  (.getItem async-storage (pr-str key)
    (fn [error result]
      (if-not error
        (try
          (let [data (read-string result)]
            (dispatch (conj on-success data)))
          (catch :default e
            (dispatch (conj on-failure e))))
        (dispatch (conj on-failure error))))))

(defn remove-item
  [{:keys [key on-success on-failure]
    :or   {on-success [::no-on-success]
           on-failure [::no-on-failure]}}]
  (.removeItem async-storage (pr-str key)
    (fn [error]
      (if-not error
        (dispatch on-success)
        (dispatch (conj on-failure error))))))

(reg-fx ::set-item set-item)
(reg-fx ::get-item get-item)
(reg-fx ::remove-item remove-item)
