(ns undefined.views.common
  (:use [noir.fetch.remotes]
     [undefined.auth :only [is-admin?]]
     [noir.core :only [defpage]]
     ;[undefined.misc :only [doall-recur]]
     )
  (:require [net.cgrand.enlive-html :as html]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Page composition:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(html/defsnippet article "templates/article.html" [:div.whole-article]
      [uid category title date article tags categories authors]
      [:div.whole-article] (html/set-attr :id (str "article_" uid))
      [:.article-title :a] (html/do-> (html/content title)
                                 (html/set-attr :href (str (name category) "-article/" uid))
                                 (html/set-attr :data-href (str (name category) "-article"))
                                 (html/set-attr :data-args (str uid)))
      [:.article-date]     (html/content date)
      [:.article]          (html/append article)
      [:.tags]             (html/content tags)
      [:.categories]       (html/content categories)
      [:.authors]          (html/content authors)
      [:.admin]            (html/append (if (is-admin?)
                                          [{:tag :button :attrs {:class "btn_upd" :value (str uid)} :content "Edit"}
                                           {:tag :button :attrs {:class "btn_del" :value (str uid)} :content "Delete"}])))

(html/defsnippet product "templates/product.html" [:div.whole-article]
      [title link article sc]
      [:.article-title :a]   (html/do-> (html/content title) (html/set-attr :href link))
      [:.product-desc]       (html/content article)
      [:.product-screenshot] (html/content sc))

(html/defsnippet login "templates/login.html" [:form]
      [])

(html/defsnippet newarticle "templates/new_article.html" [:form.newarticle]
      [authors categories title body tags uid]
      [:.inp_title]       (html/set-attr :value title)
      [:.txt_body]        (html/content body)
      [:.inp_tags]        (html/set-attr :value tags)
      [:.cbx_authors]     (html/html-content (reduce str (map #(str "<input type=\"checkbox\" class=\"cbx_auth\" value=\"" (:uid %) "\">" (:name %) "</input><br/>") authors)))
      [:.cbx_categories]  (html/html-content (reduce str (map #(str "<input type=\"checkbox\" class=\"cbx_cat\" value=\"" (:uid %) "\">" (:label %) "</input><br/>") categories)))
      [:.btn_add_article] (html/set-attr :value uid)
      [:.btn_rst]         (html/set-attr :value uid))

(html/defsnippet metadata "templates/metadata.html" [:#metadata]
      [data]
      [:#metadata] (apply html/do-> (map #(html/set-attr % (% data)) (keys data))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Page skeleton:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(html/deftemplate base "templates/index.html"
      [content]
      [:.admin]        (html/add-class "hidden")
      [:title]         (html/content "Undefined Development")
      [:#page-wrapper] (html/append content))

(html/defsnippet page "templates/page.html" [:#page]
      [title content & [optional]]
      [:#title]   (html/content title)
      [:#content] (html/do-> (html/append content)
                             (html/append (metadata (:metadata optional))))
      [:#bottom]  (html/append (:bottom optional)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Page loading:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def page-inits {})

;; used by index.clj:
(def page-404 (page "404"
                    {:tag :div :attrs {:class "whole-article"}
                     :content [{:tag :center :content [{:tag :img :attrs {:src "/img/deadlink.png"}}]}]}
                    {:metadata {:data-init-page "404"}}))

(defremote get-page [href & [args]]
  (apply str (html/emit* (if-let [f (page-inits href)]
                           (f href args)
                           page-404))))

;; WARNING: not thread safe.
(defn register-page-init! [name func]
  (def page-inits (into page-inits {name func})))

(defmacro add-page-init! [name fun & [arg]]
  `(do
     (register-page-init! ~name ~fun)
     ~(if arg
        `(defpage ~(str "/" name "/:" arg ) {:keys [~(symbol arg)]}
           (base (~fun ~name ~(symbol arg))))
        `(defpage ~(str "/" name) []
           (base (~fun ~name nil))))))
