(ns markov-text-gen.core
  (:require [clojure.string :as s]
            [clojure.java.io :as io])
  (:gen-class))

;; need this to spit big maps to file, dont print them to console
(set! *print-length* nil)

(defn- load-words-from-resource [file]
  (println "loading " file)
  (assoc {:file file} :words
         (filter #(not (= % " " ""))
                 (-> file
                     (io/resource)
                     (io/file)
                     (slurp)
                     ;; remove formatting, and parens, since we cant count open/closed parens
                     (s/lower-case)
                     (s/replace #"[\n|—|\(|\)\"\'\`\´\t\“\-\;\:]" " ")
                     (s/replace #"[ ]+" " ")
                     (s/split #" ")))))

(defn- create-markov-chain [state-size {words :words :as ctx}]
  (let [part (partition state-size words)
        keys (map (partial s/join " ") part)
        values (rest (map #'first part))
        values (remove #(= % " " "") values)]
    (-> ctx
        (dissoc :words)
        (assoc :state-size state-size :db (zipmap keys values)))))

(defn- save-markov-chain-to-file [markov]
  (let [file (s/replace (:file markov) #".txt" ".markov")]
    (println "saving " (:file markov))
    (spit (str "markov-files/" file) (with-out-str (pr markov)))))

(defn- load-markov-chain-from-file [file]
  (println "loading " file)
  (read-string (slurp file)))

(defn- merge-markov-chains [chains]
  (->> chains
       (pmap :db)
       (reduce #(merge-with (comp flatten vector) %1 %2))))

(defn- generate [iterations initial-words markov-chain]
  (let [story (atom (into [] (s/split initial-words #" ")))]
    (dotimes [i iterations]
      (let [db (:db markov-chain)
            state-size (:state-size markov-chain)
            items (take-last state-size @story)
            pattern (s/join " " items)
            result (get db pattern "")]
        (cond
          (seq? result) (swap! story conj (rand-nth result))
          (not (empty? result)) (swap! story conj result)
          :else nil)))
    (s/join " " @story)))

(defn -main [& args]
  (let [files ["moby-dick" "frankenstein" "alice"
               "grimms" "dracula" "sherlock"
               "huckleberry" "treasure-island" "oz"
               "baskerville"]]
    (let [state-size 2]
      (time
       (->>
        files
        (map #(str % ".txt"))
        (map #'load-words-from-resource)
        (map (partial create-markov-chain state-size))
        (map #'save-markov-chain-to-file)
        (pr-str)
        )))

    (let [state-size 2]
      (time
       (->>
        files
        (map #(str "markov-files/" % ".markov"))
        (filter #(.exists (io/file %)))
        (pmap #'load-markov-chain-from-file)
        (merge-markov-chains)
        (assoc {:state-size state-size} :db)
        (generate 50 "it was")
        ))))
  )
