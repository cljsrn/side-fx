(ns side-fx.fetch
  (:require
   [clojure.string :as string]
   [re-frame.core :refer [reg-fx dispatch]]))

(defn fetch-effect
  [{:keys [method uri headers body on-success on-failure]
    :or   {on-success [::no-on-success]
           on-failure [::no-on-failure]}}]
  (let [input (cond-> {:method (-> method name string/upper-case)}
                headers (assoc :headers headers)
                body (assoc :body body))]
    (-> (js/fetch uri (clj->js input))
      (.then #(if (.-ok %)
                (.text %)
                (throw (ex-info "Failed response"
                         {:status      (.-status %)
                          :status-text (.-statusText %)}))))
      (.then (fn [text]
               (try
                 (js/JSON.parse text)
                 (catch :default e
                   (throw (ex-info "Failed parsing JSON"
                            {:cause    e
                             :response text}))))))
      (.then #(dispatch (conj on-success %)))
      (.catch #(dispatch (conj on-failure %))))))

(reg-fx ::fetch fetch-effect)
