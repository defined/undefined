(ns undefined.sql
  (:refer-clojure :exclude [extend])
  (:require [clojure.string :as string]
            [clj-time.format :as time-format]
            [noir.session :as session])
  (:use [clj-time.core :only [now]]
        [undefined.config :only [get-config]]
        [undefined.misc   :only [get_keys]]
        [noir.fetch.remotes]
        [korma.db]
        [korma.core]
        [undefined.content :only [str-to-int]]
        [undefined.auth :only [is-admin?]]))


(defdb undef-db (postgres {:db "undefined"
                           :user "web"
                           :password "password";"droptableusers"
                           ;;OPTIONAL KEYS
                           :host "127.0.0.1"
                           :port "5432"
                           :delimiters "" ;; remove delimiters
                           :naming {:keys string/lower-case
                                    ;; set map keys to lower
                                    :fields string/upper-case}}))

(defn init-db-connection
  "Defines the database connection to use from within Clojure."
  [config]
  (def undef-db (create-db (postgres {:db (:database config)
                                      :user "web"
                                      :password "password" ;droptableusers"
                                      ;;OPTIONAL KEYS
                                      :host "127.0.0.1"
                                      :port "5432"
                                      :delimiters "" ;; remove delimiters
                                      :naming {:keys string/lower-case
                                               ;; set map keys to lower
                                               :fields string/upper-case}})))
  (default-connection undef-db))


(defentity projects
  (table :projects)
  (pk :uid)
  (entity-fields :uid :title :description :link :screenshot :pin)
  (database undef-db))

(defentity comments
  (table :comments)
  (pk :uid)
  (entity-fields :uid :content :authid :artid :birth :edit)
  (database undef-db))

(defentity tags
  (table :tags)
  (pk :uid)
  (entity-fields :id :label)
  (database undef-db))

(defentity authors
  (table :authors)
  (pk :uid)
  (entity-fields :username)
  (database undef-db))

(defentity roles
  (table :roles)
  (pk :uid)
  (entity-fields :label)
  (database undef-db))

(defentity categories
  (table :categories)
  (pk :uid)
  (entity-fields :label)
  (database undef-db))

(defentity author_roles
  (table :author_roles)
  (pk :roleid)
  (has-many roles   {:fk :uid})
  (entity-fields :roles.label))

(defentity article_tags
  (table :article_tags)
  (pk :tagid)
  (has-many tags {:fk :uid})
  (entity-fields :artid :tagid)
  (database undef-db))

(defentity article_categories
  (table :article_categories)
  (pk :catid)
  (has-many categories {:fk :uid})
  (entity-fields :artid :catid)
  (database undef-db))

(defentity article_authors
  (table :article_authors)
  (pk :authid)
  (has-many authors {:fk :uid})
  (entity-fields :artid :authid)
  (database undef-db))

(defentity articles
  (table :articles)
  (pk :uid)
  (entity-fields :uid :title :body :birth)
  (database undef-db))

;SELECT

(defn tag_cloud []
  (select article_tags
          ;(aggregate (count :*) :artid :cnt)
          (fields :tags.label :tags.uid)
          (group :tags.label :tags.uid)
          (aggregate (count :*) :cnt :tags.label)
          (join tags (= :tags.uid :article_tags.tagid))))

(defn articles_by_tags [id off n]
  (select article_tags
          (fields [:article_tags.artid :uid]
                  [:articles.title :title] [:articles.body :body] [:articles.birth :birth])
          (join articles (= :articles.uid :article_tags.artid))          
          (where {:article_tags.tagid id})
          (limit n)
          (offset off)
          (order :articles.birth :DESC)))

(defn select_articles [off n cat]
  (select article_categories
          (fields :articles.title :articles.body :articles.birth :articles.uid)
          (join articles (= :article_categories.artid :articles.uid))
          (join categories (= :categories.uid :article_categories.catid))
          (limit n)
          (offset off)
          (where {:categories.label cat})
          (order :articles.birth :DESC)))

(defn comment_count_by_article [id]
  (select comments
          (aggregate (count :*) :cnt)
          (where {:artid id})))

(defn select_article [id]
  (select article_categories
          (fields :articles.title :articles.body :articles.birth :articles.uid)
          (join articles (= :article_categories.artid :articles.uid))
          (join categories (= :categories.uid :article_categories.catid))
          (modifier "distinct")
          (where {:artid id})
          (order :articles.birth :DESC)))

(defn select_tags [& [id]]
  (if id
    (select tags 
            (where {:uid id}))
    (select tags)))

(defn tags_by_article [id]
  (select article_tags
          (fields :tags.uid :tags.label)
          (join tags)
          (where {:artid id})))

(defn tags_by_label [label]
  (select tags
          (where {:label [like (str "%" label "%")]})))

(defn select_categories [] (select categories))

(defn categories_by_article [id]
  (select article_categories
          (fields :categories.label)
          (join categories)
          (where {:artid id})))

(defn select_authors [] (select authors))

(defn authors_by_article [id]
  (select article_authors
          (fields :authors.username)
          (join authors)
          (where {:artid id})))

(defn get_user [& {:keys [id username] :or {id nil username nil}}]
  (let [[col val] (if (nil? username)
                    [:uid id]
                    [:username username])]
    (select authors
            (join author_roles (= :author_roles.authid :authors.uid))
            (join roles (= :roles.uid :author_roles.roleid))
            (fields [:authors.uid :uid] [:authors.username :username] [:authors.password :pass]
                    [:authors.salt :salt] [:roles.label :roles])
            (where {col val}))))

(defn select_projects [] (select projects))

(defn get_user_roles [id]
  (select author_roles
          (fields :roles.label)
          (join roles)
          (where {:authid id})))

(defn is_user_admin? [id]
  (select author_roles
          (join roles)
          (where {:authid id
                  :roles.label "Admin"})))

(defn comments_by_article [id]
  (select comments
          (fields :uid :content [:authors.username :author] :birth :edit)
          (join authors (= :authors.uid :comments.authid))
          (aggregate (count :*) :cnt :comments.uid)
          (group :authors.username :birth :edit)
          (order :birth :ASC)
          (where {:artid id})))

;INSERT

;TODO this has to be prettyfiable
(defn weed_tags [tag_input artid]
  (let [tag_array       (distinct (clojure.string/split tag_input #" "))
        existing_tags   (map #(:label %) (select_tags))
        filtered_tags   (clojure.set/difference (set tag_array) (set existing_tags))
        get_tagid       (fn [x] (str (:uid ((select tags
                                                    (fields :uid :label)
                                                    (where {:label x})) 0))))]
    (doseq [x filtered_tags] (insert tags (values {:label x})))
    (doseq [x tag_array] (insert article_tags (values {:artid artid :tagid (Integer/parseInt (get_tagid x))})))))

;TODO transaction
(defn insert_article [current-id title body tags authors categories]
  (if (is-admin? current-id)
    (let [artid     (:uid (insert articles (values {:title title :body body})))
          auths     (get_keys authors)
          cats      (get_keys categories)]
      (doseq [x auths]  (insert article_authors     (values {:artid artid :authid (Integer/parseInt x)})))
      (doseq [x cats]   (insert article_categories  (values {:artid artid :catid (Integer/parseInt x)})))
      (weed_tags tags artid)
      artid)))

;TODO Add authentication
(defn insert_comment [id author content]
  (if (is-admin? author)
    (insert comments (values {:artid id :authid author :content content}))))

;UPDATE
;TODO don't delete/re-insert tags/cats/auths
;TODO actually... would there be that much of a perf improvement..? isn't it worse to check for existing values?
(defn update_article [current-id uid title body tags authors categories]
  (if (is-admin? current-id)
    (let [auths     (get_keys authors)
          cats      (get_keys categories)]
    (transaction
      (update articles
              (set-fields {:title title :body body})
              (where {:articles.uid uid}))
      (delete article_tags
              (where {:artid uid}))
      (delete article_authors
              (where {:artid uid}))
      (delete article_categories
              (where {:artid uid}))
      (weed_tags tags uid)
      (doseq [x auths]  (insert article_authors     (values {:artid uid :authid (Integer/parseInt x)})))
      (doseq [x cats]   (insert article_categories  (values {:artid uid :catid (Integer/parseInt x)})))))))

(defn update_comment [uid author content]
;  (if (is-admin? author)
    (update comments
            (set-fields {:content content :edit (java.sql.Timestamp/valueOf
                                                  (time-format/unparse
                                                    (time-format/formatter "yyyy-MM-dd HH:mm:ss")
                                                    (from-time-zone (now) (time-zone-for-offset -2))))})
            (where {:uid uid})))


;DELETE
(defn delete_article [id uid]
  (if (is-admin? id)
    (delete articles
            (where {:uid uid}))))

(defn delete_comment [id uid]
  (id (is-admin? id)
      (delete comments
              (where {:uid uid}))))


;; Remotes

(defremote insert_article_rem [title body tags authors categories] (insert_article (session/get :id) title body tags authors categories))
(defremote update_article_rem [uid title body tags authors categories] (update_article (session/get :id) (str-to-int uid) title body tags authors categories))
(defremote insert_comment_rem [artid authid content] (insert_comment artid authid content))
(defremote update_comment_rem [comid authid content] (update_comment comid authid content))
(defremote delete_comment_rem [adminid comid] (delete_comment adminid comid))
(defremote comment_count_rem  [id] (comment_count_by_article id))
;(defremote tags_by_article_rem [id] (tags_by_article (str-to-int id)))
;(defremote select_article_rem [id] (select_article (str-to-int id)))
(defremote delete_article_rem [uid] (delete_article (session/get :id) (str-to-int uid)))
;(defremote select_authors_rem [] (select authors))
;(defremote select_categories_rem [] (select categories))
;(defremote get_user_rem [& {:keys [id username] :or {id nil username nil}}] (get_user :id id :username username))
;(defremote select_projects_rem [] (select_projects))
;(defremote get_user_roles_rem [id] (get_user_roles id))
;(defremote is_user_admin_rem? [id] (is_user_admin? id))

;(defremote tag_cloud_rem [] (tag_cloud))
;(defremote articles_by_tags_rem [id] (articles_by_tags id))
