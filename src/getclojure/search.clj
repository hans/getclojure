(ns getclojure.search
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [getclojure.util :as util]))

(def mappings
  {:sexp
   {:properties
    {:id {:type "integer" :store "yes"}
     :input {:type "string" :store "yes" :analyzer "clojure_code" :tokenizer "clojure_tokenizer" :filter "clojure_filter"}
     :output {:type "string" :store "yes" :analyzer "clojure_code" :tokenizer "clojure_tokenizer" :filter "clojure_filter"}
     :value {:type "string" :store "yes" :analyzer "clojure_code" :tokenizer "clojure_tokenizer" :filter "clojure_filter"}}}})

(def clojure-analyzer
  {:clojure_code {:type "custom"
                  :tokenizer "lowercase"
                  :filter ["lowercase" "clojure_filter"]}})

(def clojure-tokenizer
  {:clojure_tokenizer {:type "lowercase"}})

(def clojure-filter
  {:clojure_filter {:type "lowercase"}})

(defn create-getclojure-index []
  (when-not (esi/exists? "getclojure")
    (esi/create "getclojure"
                :settings {:index {:analysis {:analyzer clojure-analyzer
                                              :tokenizer clojure-tokenizer
                                              :filter clojure-filter}}}
                :mappings mappings)))

(defn add-to-index [env sexp-map]
  (esd/put (name env) "sexp" (util/uuid) sexp-map))

;; :from, :size
(defn search-sexps [q page-num]
  (let [offset (* (Integer/parseInt page-num) 25)]
    (esd/search "getclojure"
                "sexp"
                :query (q/query-string :query q
                                       :fields ["input^5" :value :output])
                :from offset
                :size 25)))

(defn get-num-hits [q page-num]
  (get-in (search-sexps q page-num) [:hits :total]))

(defn get-search-hits [result-map]
  (map :_source (get-in result-map [:hits :hits])))

(defn search-results-for [q page-num]
  (get-search-hits (search-sexps q page-num)))

(comment "Development"
  (esr/connect! "http://127.0.0.1:9200"))