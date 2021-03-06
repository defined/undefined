(ns undefined.views.news
  (:require [net.cgrand.enlive-html :as html])
  (:use [undefined.views.common :only [add-page-init! page new-article new-comment please-log-in article user-comment base a-link]]
        [undefined.sql :only [select_articles select_article select_authors select_categories
                              tags_by_article articles_by_tags select_tags
                              categories_by_article authors_by_article
                              comments_by_article comment_count select_comment]]
        [undefined.auth :only [is-admin? is-author? username]]
        [undefined.misc :only [format-date get_labels]]
        [undefined.content :only [remove-unsafe-tags str-to-int]]
        [noir.fetch.remotes]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Helper funs;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn blog-nav [prev next category type id offset]
  (let [link (str "/" (name category) (when (= type :single) "-article"))
        args #(if (= type :tag)
                (str "/" id "/" %1)
                (str "/" %1))]
    [(when prev
       {:tag :a :attrs {:href (str link (args prev))} :content "Previous"})
     (when next
       {:tag :a :attrs {:href (str link (args next)) :style "float: right"} :content "Next"})]))

(defn get-category [href type]
  (cond (= type :tag)       :tag
        (= (first href) \b) :blog
        :else               :news))

(defn mk-blog-cat-title [category & [id]]
  (cond
    (= category :blog) "Undefined's Technical Blog"
    (= category :news) "Undefined's Latest News"
    id                 (str (:label (first (select_tags id))))
    :else              "Undefined's Articles"))

(defn mk-tag-link [tag]
  (a-link (str (:label tag) " ") {:href (str "/tag/" (:uid tag))}))

(defn mk-comment [user-id comment]
  (user-comment (:uid comment) user-id (:authid comment)
                (:author comment)
                (format-date (:birth comment)) (when (:edit comment)
                                                 (format-date (:edit comment)))
                (remove-unsafe-tags (:content comment))))

(defn mk-comments [user-id article-uid]
  (concat (map (partial mk-comment user-id) (comments_by_article article-uid))
          (if (username user-id)
            (new-comment article-uid 0 nil)
            (please-log-in))))

(defn mk-comment-count [type uid]
  (str "Comment Count: " (:cnt (first (comment_count type uid)))))

(defremote mk-comment-count-rem [type uid]
  (mk-comment-count type uid))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  News;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mk-article [uid category admin? title birth body comments]
  (article uid category admin?
           title (format-date birth) (remove-unsafe-tags body)
           {:tag :span :content (cons "Tags: " (mapcat mk-tag-link (tags_by_article uid)))}
           (str "Authors: " (get_labels (authors_by_article uid) :username))
           (mk-comment-count :article uid) comments))

(defn news-page [user-id href type [arg1 arg2]]
  (let [[arg1 arg2]                [(str-to-int arg1) (str-to-int arg2)]
        category                   (get-category href type)
        nb-articles                10
        [offset articles comments] (condp = type
                                     :single [nil         (select_article arg1) (mk-comments user-id arg1)]
                                     :page   [arg1        (select_articles arg1 (inc nb-articles) (name category)) nil]
                                     :tag    [(or arg2 0) (articles_by_tags arg1 arg2 (inc nb-articles))] nil)
        [nx articles]              (if (> (count articles) nb-articles)
                                     [(+ offset nb-articles) (drop-last articles)]
                                     [nil articles])
        pv                         (when (and offset (> offset 0))
                                     (- offset nb-articles))
        admin?                     (is-admin? user-id)]
    (page (mk-blog-cat-title category arg1)
          (map #(mk-article (:uid %) category admin? (:title %) (:birth %) (:body %) comments)
               articles)
          {:bottom (blog-nav (if (and pv (neg? pv)) 0 pv) nx category type arg1 offset)
           :metadata {:data-init-page "news"}})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Updates;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-article-div [user-id href uid]
  (let [[article] (select_article uid)]
    (new-article (select_authors) (select_categories) (:title article) (:body article)
                 (get_labels (tags_by_article (:uid article)) :label) (:uid article)
                 (authors_by_article (:uid article)) (categories_by_article (:uid article)))))

(defn update-comment-div [user-id href uid]
  (let [[comment] (select_comment uid)]
    (new-comment nil uid (:content comment))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  routes;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(add-page-init! "update-article-div" update-article-div)
(add-page-init! "update-comment-div" update-comment-div)
(add-page-init! "refresh-article-div" #(let [[{:keys [uid title birth body]}] (select_article (str-to-int %3))
                                             [{:keys [content]}]              (mk-article uid :blog (is-admin? %1) title birth body nil)]
                                         content)
                1) ;; FIXME; decide on a methode ; get contents from wrapper (refresh-article-div) and replace content in cljs, or send with wrapper and hide, then substitute and hide and show in cljs.
(add-page-init! "refresh-comment-div" #(mk-comment %1 (first (select_comment (str-to-int %3)))) 1)
(add-page-init! "fetch-comment-div" #((html/add-class "hidden") (first (mk-comment %1 (first (select_comment (str-to-int %3)))))) 1)

(add-page-init! "news" #(news-page %1 %2 :page [(or 0 %3)])) ;; always evals to 0 but reference %3 for compiler.
(add-page-init! "blog" #(news-page %1 %2 :page [(or 0 %3)]))
(add-page-init! "blog-article" #(news-page %1 %2 :single %3) 1)
(add-page-init! "news-article" #(news-page %1 %2 :single %3) 1)
(add-page-init! "tag-article"  #(news-page %1 %2 :single %3) 1)
(add-page-init! "news" #(news-page %1 %2 :page %3) 1)
(add-page-init! "blog" #(news-page %1 %2 :page %3) 1)

(add-page-init! "tag"  #(news-page %1 %2 :tag %3) 1)
(add-page-init! "tag"  #(news-page %1 %2 :tag %3) 2)
