(ns undefined.views.products
  (:require [net.cgrand.enlive-html :as html])
  (:use [undefined.views.common :only [add-page-init! page product]]
     [undefined.content :only [remove-unsafe-tags str-to-int]]
     [undefined.sql :only [select_products]]
     [noir.fetch.remotes]))

(defn products-page [name product-id]
  (let [title       "Undefined's Products"
        products    (select_products)]
    (page title 
          (map #(product (:title %) (:link %) (:description %) (:screenshot %)) products))))

(add-page-init! "products" products-page)
