(ns brew-data.analysis
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))


(def recipe-file "./resources/recipeData.csv")
(def style-file "./resources/styleData.csv")

(defn with-csv
  "Return a function that execute body on the parsed csv file"
  [res]
  (fn [body] (with-open [reader (io/reader res)]
               (body (csv/read-csv reader)))))

(defn with-csv-lst
  "Return a function that execute body on the parsed csv file"
  [res]
  (fn [body] (with-open [reader (io/reader res)]
               (->> reader
                    csv/read-csv
                    body
                    doall))))

(defn ipa-rows
  "Return a list of all IPA style with labels"
  []
  ((with-csv-lst style-file)
   (fn [data] (->> data
                   (filter #(re-matches #"(?i).*ipa.*" (first %)))))))

(defn headers []
  ((with-csv-lst recipe-file)
   (fn [data] (->> data
                   first))))


(defn take-sample [num]
  ((with-csv-lst recipe-file)
   (fn [data]
     (take num data))))

(defn in-type-id [row typeid-vals]
  (not (nil? ((->> typeid-vals
                   (map second)
                   set) (get row 4)))))

(defn get-ipa-rows []
  ((with-csv-lst recipe-file)
   #(->> %
         (filter (fn [r] (not (nil? ((->> (ipa-rows)
                                          (map second)
                                          set) (get r 4)))))))))

(comment

  (get (headers) 4)

  (count (take-sample 1000))

  )
