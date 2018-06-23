(ns markov-text-gen.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:gen-class))

(defn- load-words-from-resource
  "Loads words from a file. Creates a vector of cleaned words by reading a file
  from disk, and applying formatting."
  [file]
  (println "loading resource: " file)
  (assoc {:file file} :words
         (filter #(not (= % " " ""))
                 (-> file
                     (io/resource)
                     (io/file)
                     (slurp)
                     (str/lower-case)
                     ;; remove formatting and special characters from input text
                     (str/replace #"[^a-z|^ |^.|^,]*" "")
                     ;; compress excess white space
                     (str/replace #"[ ]+" " ")
                     (str/split #" ")))))

(defn- create-markov-chain
  "Creates a markov-chain of size STATE-SIZE, from the list of candidate WORDS."
  [state-size {words :words :as ctx}]
  (let [part (partition state-size words)
        keys (map (partial str/join " ") part)
        values (rest (map #'first part))]
    (-> ctx
        (dissoc :words)
        (assoc :state-size state-size :db (zipmap keys values)))))

(defn- save-markov-chain-to-file
  "Saves a markov-chain to a file on disk."
  [markov]
  (let [file (:file markov)
        file (str/replace file #".txt" ".markov")
        file (str "markov-files/" file)]
    (println "saving " file)
    (with-open [out-file (io/writer file)]
      (binding [*print-length* nil
                *out* out-file]
        (pr markov)))))

(defn- load-markov-chain-from-file
  "Loads a markov-chain from a file on disk."
  [file]
  (println "loading markov chain: " file)
  (read-string (slurp file)))

(defn- merge-markov-chains
  "Merges several markov-chains together into a single chain. if a key has
  duplicate values they are perserved by making the value for that key a vector
  of those."
  [chains]
  (->> chains
       (pmap :db)
       (reduce #(merge-with (comp flatten vector) %1 %2))))

(defn- generate
  "tries to generate a sentence starting with INITAL-WORDS, taking ITERATIONS, and
  using MARKOV-CHAIN as the source of new content."
  [iterations initial-words markov-chain]
  (let [story (atom (into [] (str/split initial-words #" ")))]
    (dotimes [i iterations]
      (let [db (:db markov-chain)
            state-size (:state-size markov-chain)
            items (take-last state-size @story)
            pattern (str/join " " items)
            result (get db pattern "")]
        (cond
          ;; if there are multiple choices in the chain for some input, pick a random one
          (seq? result) (swap! story conj (rand-nth result))
          (not (empty? result)) (swap! story conj result)
          :else nil ;; short-circuits if there is not match in the markov chain.
          )))
    (str/join " " @story)))

(defn -main [& args]
  (let [files ["moby-dick" "frankenstein" "alice"
               "grimms" "dracula" "sherlock"
               "huckleberry" "treasure-island" "oz"
               "baskerville"]
        state-size 2]
    (println "--------")

    (time
     (->>
      files
      (map #(str % ".txt"))
      (map #'load-words-from-resource)
      (map (partial create-markov-chain state-size))
      (map #'save-markov-chain-to-file)
      (prn)
      ))

    (time
     (->> files
          (map #(str "markov-files/" % ".markov"))
          (filter #(.exists (io/file %)))
          (map #'load-markov-chain-from-file)
          (merge-markov-chains)
          (assoc {:state-size state-size :file "merged.txt"} :db)
          (save-markov-chain-to-file)))

    (time
     (->>
      "markov-files/merged.markov"
      load-markov-chain-from-file
      (generate 50 "i am")
      (println)
      )))
  )
