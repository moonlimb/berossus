(ns berossus.rocks.your.data.middleware
  (:require [taoensso.timbre :as t]
            [clojure.stacktrace :as st]
            [berossus.rocks.your.data.config :refer [get-config]]
            [berossus.rocks.your.data.services :refer [registered]]
            [ring.util.response :refer [response]]
            [selmer.parser :refer [render-file]]))

(defn wrap-exception [f]
  (fn [request]
    (try (f request)
      (catch Exception e
        (let [cause (with-out-str (st/print-cause-trace e 1))
              stack (with-out-str (st/print-stack-trace e 3))]
          (try
            (t/error e "Exception caught by middleware")
            (catch Exception new-e
              (println "We failed to log the exception " e " with exception " new-e)))
          {:status 500
           :body (str "Exception thrown: " e
                      "\n\nwith cause: \n" cause
                      "\nwith stack " stack)})))))

(defn template-renderer [context]
  (let [template (:template context)
        data     (:data context)]
    (response (render-file template data))))

(defn edn-renderer [context]
  (response (pr-str (:data context))))

(def export-funcs
  {"application/identity" identity
   "text/edn"             edn-renderer
   "*/*"                  template-renderer
   "text/html"            template-renderer})


(defn exporter [request]
  (let [accept (get-in request [:headers "accept"])
        [[_ format-fn]] (filter #(.contains accept (first %)) (seq export-funcs))]
    format-fn))

(defn wrap-export [f]
  (fn [request]
    (let [export-fn (exporter request)
          response (f request)
          status (and (map? response) (:status response))]
      (if (and status (not= 200 status))
        response
        (export-fn response)))))

(defn inject-services [f]
  (fn [request]
    (f (assoc request :services @registered))))

(defn validate-service [f]
  (fn [request]
    (let [{:keys [service]} (:params request)
          services          (:services request)]
      (if-not (contains? services (keyword service))
        {:status 404
         :body "Service not found, did you start the API server with a service alias by that name?"}
        (f request)))))

(defn wrap-service [f]
  (-> f validate-service inject-services))
