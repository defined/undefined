(ns undef.news
  (:require [fetch.remotes :as remotes]
     [enfocus.core :as ef])
  (:require-macros [fetch.macros :as fm]
     [enfocus.macros :as em])
  (:use [undef.pages :only [add-page-init! page-click]]))

(defn newspage [href & [args]]
  (em/at js/document
      [:.btn_del] (em/do->
                      (em/remove-listener :click)
                      (em/listen :click (fn [e]
                                          (let [uid (em/from (.-currentTarget e) (em/get-attr :value))]
                                            (when (js/confirm (str "This will PERMANENTLY erase the article #" uid " from the database."))
                                              (fm/letrem [res (delete_article_rem uid)]
                                                  (em/at js/document [(str ":#article_" uid)] (em/substitute ""))))))))
      [:.btn_upd] (em/do->
                      (em/remove-listener :click)
                      (em/listen :click (fn [e]
                                          (let [uid    (int (em/from (.-currentTarget e) (em/get-attr :value)))
                                                sel    (str ":#article_" uid)]
                                            (fm/letrem [div (get-page "news-update-article-div" uid)]
                                                (em/at js/document
                                                    [sel] (em/html-content div)
                                                    [:form] (em/listen :submit
                                                                (fn [e]
                                                                  (.preventDefault e)
                                                                  (let [article (em/from js/document
                                                                                    :title [(str sel " .inp_title" )] (em/get-prop :value)
                                                                                    :body  [(str sel " .txt_body" )]  (em/get-prop :value)
                                                                                    :tags  [(str sel " .inp_tags" )]  (em/get-prop :value))
                                                                        newauths (zipmap
                                                                                   (em/from (em/select [".cbx_auth"]) (em/get-prop :value))
                                                                                   (em/from (em/select [".cbx_auth"]) (em/get-prop :checked)))
                                                                        newcats  (zipmap
                                                                                   (em/from (em/select [".cbx_cat"]) (em/get-prop :value))
                                                                                   (em/from (em/select [".cbx_cat"]) (em/get-prop :checked)))]
                                                                    (fm/letrem [res (update_article_rem uid
                                                                                                        (:title article)
                                                                                                        (:body article)
                                                                                                        (:tags article)
                                                                                                        newauths
                                                                                                        newcats)
                                                                                div (get-page "news-refresh-article-div" uid)]
                                                                        (em/at js/document [sel] (em/substitute div))
                                                                        (newspage href args)))))))))))));FIXME only refresh the new buttons?

(add-page-init! "news" newspage)
