(ns markov-text-gen.core
  (:require [clojure.string :as s])
  (:gen-class))

(defn- load-words [file]
  (-> file
      (clojure.java.io/resource)
      (clojure.java.io/file)
      (slurp)
      ;; remove formatting, and parens, since we cant count open/closed parens
      (s/replace #"[\n|—|\(|\)]" " ")
      (s/split #" ")))

(defn- create-markov-chain [state-size words]
  (let [part (partition state-size words)
        keys (map (partial s/join " ") part)
        values (rest (map #'first part))]
    {:state-size state-size :db (zipmap keys values)}))

(defn- generate [iterations initial-words markov-chain]
  (let [story (atom (into [] (s/split initial-words #" ")))]
    (dotimes [i iterations]
      (let [items (take-last (:state-size markov-chain) @story)
            pattern (s/join " " items)
            result (get (:db markov-chain) pattern "")]
        (if (seq? result)
          (swap! story conj (rand-nth result))
          (swap! story conj result))))
    (s/join " " @story)))

(defn -main [& args]
  (->>
   '("moby-dick.txt" "frankenstein.txt" "alice.txt")
   ;; '("test.txt" "test2.txt")
   (map #'load-words)
   (map (partial create-markov-chain 1))
   (map :db)
   (reduce #(merge-with (comp flatten vector) %1 %2))
   (assoc {:state-size 1} :db)
   (generate 50 "once upon")))
