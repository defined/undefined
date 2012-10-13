(ns undef.news
  (:require [fetch.remotes :as remotes]
            [enfocus.core :as ef])
  (:require-macros [fetch.macros :as fm]
                   [enfocus.macros :as em])
  (:use [undef.pages :only [add-page-init! page-click]]
        [undef.misc :only [restore-height]]))


(defn newspage [href & [args]]
  (letfn [(submit [type sel uid]
            (letfn [(animate-replace [div]
                      (em/at js/document
                             [sel] (em/chain (em/resize :curwidth 0 200) ;; FIXME might make a function out of this (defn up-down-change-elt [& funs to add to chain])
                                             (em/substitute div)
                                             (ef/chainable-standard #(em/at %
                                                                            [:.btn_del] (em/listen :click (delete-button type))
                                                                            [:.btn_upd] (em/listen :click (update-button type))))
                                             (restore-height 200))))]
              (if (= type :article)
                (fn [e]
                  (.preventDefault e)
                  (let [article (em/from js/document
                                         :title     [sel :.inp_title] (em/get-prop :value)
                                         :body      [sel :.txt_body]  (em/get-prop :value)
                                         :tags      [sel :.inp_tags]  (em/get-prop :value)
                                         :auths-val [sel :.cbx_auth]  (em/get-prop :value)
                                         :auths-c?  [sel :.cbx_auth]  (em/get-prop :checked)
                                         :cats-val  [sel :.cbx_cat]   (em/get-prop :value)
                                         :cats-c?   [sel :.cbx_cat]   (em/get-prop :checked))
                        one-c?  (partial some identity)]
                    (if (and (one-c? (:cats-c? article)) (one-c? (:auths-c? article))) ;; FIXME test title/body & add tests on server side
                      (fm/letrem [res (update_article_rem uid
                                                          (:title article)
                                                          (:body article)
                                                          (:tags article)
                                                          (zipmap (:auths-val article) (:auths-c? article))
                                                          (zipmap (:cats-val article) (:cats-c? article)))
                                  div (get-page "refresh-article-div" uid)]
                        (animate-replace div))
                      (js/alert "Check at least one author and category"))))
                (fn [e]
                  (.preventDefault e)
                  (let [comment (em/from js/document
                                         :body       [sel :txt_body] (em/get-prop :value)
                                         :article-id [sel :txt_body] (em/get-attr :data-article-id))]
                    (if (re-find #"^\s*$" "")
                      (fm/letrem [res (update_comment_rem uid article-id (:body article))
                                  div (get-page "refresh-comment-div" uid)]
                        (animate-replace div))
                      (js/alert "Your comment is empty...")))))))

          (delete-button [type]
            (fn [e]
              (let [uid (em/from (.-currentTarget e) (em/get-attr :value))
                    stype (name type)]
                (when (js/confirm (str "This will PERMANENTLY erase the " stype " from the database."))
                  (fm/letrem [res (delete_rem type uid)]
                    (em/at js/document [(str "#" stype "_" uid)] (em/resize :curwidth 0 200)))))))

          (update-button [type]
            (fn [e]
              (let [uid    (int (em/from (.-currentTarget e) (em/get-attr :value)))
                    stype  (name type)
                    sel    (keyword (str "#" stype "_" uid))]
                (fm/letrem [div (get-page "update-" stype "-div" uid)]
                  (em/at js/document
                         [sel] (em/chain (em/resize :curwidth 0 200)
                                         (em/html-content div)
                                         (ef/chainable-standard #(em/at % [:form] (em/listen :submit (submit type sel uid))))
                                         (restore-height 200)))))))]
    (em/at js/document
      [:.btn_del] (em/listen :click (delete-button :article))
      [:.btn_upd] (em/listen :click (update-button :article)))))

(add-page-init! "news" newspage)
(add-page-init! "blog" newspage)
