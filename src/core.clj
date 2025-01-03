(ns core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [grafter-2.rdf.protocols :as gpr]
            [grafter-2.rdf4j.io :as gio :refer :all]))

(defn read-csv [file-path]
  (with-open [reader (io/reader file-path)]
    (let [rows (csv/read-csv reader)
          headers (map keyword (second rows))
          data (nthrest rows 2)]
      (mapv #(zipmap headers (map (fn [value] (if (clojure.string/blank? value) nil value)) %)) data))))

(def base-uri "http://example.org/")

(def prefixes
  {"dct" "http://purl.org/dc/terms/"
   "sdo" "https://schema.org/"
   "wdt" "http://www.wikidata.org/prop/direct/"
   "xsd" "http://www.w3.org/2001/XMLSchema#"})

(defn make-uri [suffix]
  (java.net.URI. (str base-uri suffix)))

(def fields
  [[:name "https://schema.org/name" #(gpr/->LangString  % :de)]
   [:description "https://schema.org/description" #(gpr/->LangString % :de)]
   [:about "https://schema.org/about" #(gpr/->LangString  % :de)]
   [:issued "http://purl.org/dc/terms/issued" #(gpr/literal % "http://www.w3.org/2001/XMLSchema#date")]
   [:described_at "http://www.wikidata.org/prop/direct/P973" #(java.net.URI. %)]
   [:maintained_by "http://www.wikidata.org/prop/direct/P126" #(gpr/->LangString % :de)]
   [:used_by "http://www.wikidata.org/prop/direct/P1535" #(gpr/->LangString % :de)]
   [:repo "http://www.wikidata.org/prop/direct/P1324" #(gpr/->LangString % :de)]
   [:educational_stage "http://www.wikidata.org/prop/direct/P7374" #(gpr/->LangString % :de)]
   [:complies_with "http://www.wikidata.org/prop/direct/P5009" #(gpr/->LangString % :de)]
   [:format "http://www.wikidata.org/prop/direct/P2701" #(gpr/->LangString % :de)]])

(defn row-to-triples [row]
  (let [entity-uri (make-uri (get row :id))]
    (loop [fields fields
           triples []]
      (if (empty? fields)
        triples
        (let [[field uri tfn] (first fields)
              val (get row field nil)
              new-triples (if (nil? val)
                            triples
                            (conj triples (gpr/->Triple entity-uri (java.net.URI. uri) (tfn val))))]
          (recur (rest fields) new-triples))))))

(defn csv-to-rdf [file-path]
  (let [rows (read-csv file-path)]
    (mapcat row-to-triples rows)))

(defn write-rdf [triples output-path]
  (with-open [writer (io/writer output-path)]
    (let [tw (gio/rdf-writer  writer {:format :ttl
                                      :prefixes prefixes})]
      (gpr/add tw triples))))

(defn convert-csv-to-rdf [input-csv output-rdf]
  (let [triples (csv-to-rdf input-csv)]
    (write-rdf triples output-rdf)))

(comment
  (convert-csv-to-rdf "resources/Vokabulare.csv" "resources/Vokabulare.ttl"))

(defn -main [& args]
  (convert-csv-to-rdf (first args) (second args)))
