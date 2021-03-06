(ns undefined.views.auth-token
  (:require [net.cgrand.enlive-html :as html])
  (:use [undefined.sql :only [activate_user]]
        [undefined.config :only [get-config]]
        [undefined.views.common :only [add-page-init! page]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; token validation:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(add-page-init! "activate" #(page "Account Activation"
                                  {:tag :div :attrs {:class "whole-article"}
                                   :content [{:tag :div :attrs {:class "article"}
                                              :content [(activate_user %3)]}]}
                                  ;{:metadata {:data-init-page "404"}};; FIXME I decided against redirect. user should se result even if he goes to get coffee.
                                  )
                1)
