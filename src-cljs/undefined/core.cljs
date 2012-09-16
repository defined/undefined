(ns undef.core
  (:use [undef.init :only [add-init!]]
        [undef.pages :only [load-page]])
  (:require [fetch.remotes :as remotes]
            [enfocus.core :as ef])
  (:require-macros [fetch.macros :as fm]
                   [enfocus.macros :as em]))

(add-init! #(load-page "news") :last)
