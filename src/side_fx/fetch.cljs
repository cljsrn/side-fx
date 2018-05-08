(ns side-fx.fetch
  (:require
   [clojure.string :as string]
   [re-frame.core :refer [reg-fx dispatch]]))

(defmulti accept-header (fn [response-format]
                          (::type response-format)))

(defmulti process-response (fn [response-format text on-success on-failure]
                             (::type response-format)))

(defmethod accept-header :text
  [{::keys [content-type]}]
  (or content-type "*/*"))

(defmethod process-response :text
  [_ text on-success]
  (dispatch (conj on-success text)))

(defmethod accept-header :json
  [{::keys [content-type]}]
  (or content-type "application/json"))

(defn- strip-prefix [prefix text]
  (if (and prefix
           (string/starts-with? text prefix))
    (subs text 0 (count prefix))
    text))

(defmethod process-response :json
  [{::keys [prefix raw keywords?]} text on-success on-failure]
  (try
    (let [result-raw (->> text
                       (strip-prefix prefix)
                       js/JSON.parse)]
      (dispatch (conj on-success (if raw
                                   result-raw
                                   (js->clj result-raw :keywordize-keys keywords?)))))
    (catch :default e
      (dispatch (conj on-failure (ex-info "Failed parsing JSON"
                                   {:cause    e
                                    :response text}))))))

(defn fetch-effect
  [{::keys [method uri headers body response-format on-success on-failure]
    :or    {response-format {::type :text}
            on-success      [::no-on-success]
            on-failure      [::no-on-failure]}}]
  (let [input (cond-> {:method  (-> method name string/upper-case)
                       :headers (merge {:Accept (accept-header response-format)}
                                  headers)}
                body (assoc :body body))]
    (prn input)
    (-> (js/fetch uri (clj->js input))
      (.then #(if (.-ok %)
                (.text %)
                (throw (ex-info "Failed response"
                         {:status      (.-status %)
                          :status-text (.-statusText %)}))))
      (.then (fn [text]
               (process-response response-format text on-success on-failure)))
      (.catch #(dispatch (conj on-failure %))))))

(reg-fx ::fetch fetch-effect)
