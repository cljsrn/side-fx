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

(def default-response-format {::type :text})

(defn- form-method
  [{::keys [method]}]
  (-> method name string/upper-case))

(defn- form-headers
  [{::keys [headers response-format]
    :or    {response-format default-response-format}}]
  (merge {:Accept (accept-header response-format)}
    headers))

(defn- form-input
  [{::keys [body]
    :as    request}]
  (clj->js (cond-> {:method  (form-method request)
                    :headers (form-headers request)}
             body (assoc :body body))))

(defn fetch-effect
  [{::keys [uri response-format on-success on-failure]
    :or    {response-format default-response-format
            on-success      [::no-on-success]
            on-failure      [::no-on-failure]}
    :as    request}]
  (-> (js/fetch uri (form-input request))
    (.then #(if (.-ok %)
              (.text %)
              (throw {::status      (.-status %)
                      ::status-text (.-statusText %)})))
    (.then (fn [text]
             (process-response response-format text on-success on-failure)))
    (.catch #(dispatch (conj on-failure (if (map? %)
                                          %
                                          {::error %}))))))

(reg-fx ::fetch fetch-effect)
