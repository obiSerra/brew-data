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

(def with-recipe-csv (with-csv recipe-file))

(defn with-csv-lst
  "Return a function that execute body on the parsed csv file"
  [res]
  (fn [body] (with-open [reader (io/reader res)]
               (->> reader
                    csv/read-csv
                    body
                    doall))))

(defn is-ipa? [row]
  (re-matches #"(?i).*ipa.*" (first row)))

(defn ipa-rows
  "Return a list of all IPA style with labels"
  []
  ((with-csv-lst style-file)
   (fn [data] (->> data
                   (filter is-ipa?)))))

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

(defn map-csv [from to map-fn]
  (with-open [reader (io/reader from)
              writer (io/writer to)]
    (->> (csv/read-csv reader)
         (map #(map-fn %))
         (csv/write-csv writer))))


(defn col-frequencies 
  "Apply func frequencies to a column of a CSV"
  [col-idx]
  (with-recipe-csv (fn [rows] (->> rows
                                   rest
                                   (map #(get % col-idx))
                                   frequencies))))

(defn rounder 
  "Generate a function that round to a certain decimal"
  [dec]
  (let [mv (Math/pow 10 dec)]   
    (fn [n] (-> n      
                (* mv)
                Math/round
                (/ mv)))))

(def round3 (rounder 3))

(defn plato-to-sg [plato]
  (as-> plato k
    (/ k 258.2)
    (* k 227.1)
    (- 258.6 k)
    (/ plato k)
    (+ k 1)
    (round3 k)))


(defn row-to-sg [row]
  (if (= "Plato" (nth row 16))
    (-> row
        (update-in [7] #(-> % 
                            read-string
                            plato-to-sg))
        (update-in [8] #(-> % 
                            read-string
                            plato-to-sg)))
    row))

(defn unifyOG-FG
  ""
  [col-idx]
  (with-recipe-csv 
   (fn [rows] (->> rows
                   rest
                   (map #(get % col-idx))))))



(defn group-by-colums [col lst]
  (reduce
   (fn [acc val] 
     (cond (nil? (get acc (nth val col nil))) (assoc acc (nth val col) [val])
           :else (update-in acc [(nth val col)] #(conj % val))))
   {}
   lst))

(defn apply-over-groups [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn map-over-groups [f m]
  (into {} (for [[k v] m] [k (map f v)])))

(comment 
  (with-recipe-csv 
   (fn [rows] (->> rows
                   rest
                   (take 10)
                   (group-by-colums 17)
                   (map-over-groups #(nth % 3))
                   )))
  
  )
