(ns jecode.util
  (:require [noir.io :as io]
            [markdown.core :as md]))

(defn md->html
  "reads a markdown file from public/md and returns a HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))
