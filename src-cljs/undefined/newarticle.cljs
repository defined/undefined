(ns undef.newarticle
  (:use [undef.pages :only [add-page-init! page-click]])
  (:require [fetch.remotes :as remotes]
     [enfocus.core :as ef])
  (:require-macros [fetch.macros :as fm]
     [enfocus.macros :as em]))

(defn newarticlepage [href & [args]]
  (em/at js/document
      [:#inp_title] (em/focus)
      [:form] (em/listen :submit (fn [e]
                                   (.preventDefault e)
                                   (let [title      (em/from (em/select [".inp_title"]) (em/get-prop :value))
                                         body       (em/from (em/select [".txt_body"]) (em/get-prop :value))
                                         tags       (em/from (em/select [".inp_tags"]) (em/get-prop :value))
                                         authors    (zipmap
                                                      (em/from (em/select [".cbx_auth"]) (em/get-prop :value))
                                                      (em/from (em/select [".cbx_auth"]) (em/get-prop :checked)))
                                         categories (zipmap
                                                      (em/from (em/select [".cbx_cat"]) (em/get-prop :value))
                                                      (em/from (em/select [".cbx_cat"]) (em/get-prop :checked)))
                                         get_keys   (fn [m] (keys (select-keys m (for [[k v] m :when (= v true)] k))))
                                         redirect   (if (= "1" (first (get_keys categories))) "blog" "news")]
                                     (fm/letrem [res (insert_article_rem title body tags authors categories)] 
                                       (page-click redirect nil)))))))

(add-page-init! "new-article" newarticlepage)
