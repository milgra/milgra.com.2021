(ns milgracom.subs
  (:require
    [re-frame.core :as rf]))


(rf/reg-sub
  ::menuitems
  (fn [db]
    (:menuitems db)))


(rf/reg-sub
  ::blog-posts
  (fn [db]
    (:blog-posts db)))


(rf/reg-sub
  ::blog-months
  (fn [db]
    (:blog-months db)))


(rf/reg-sub
  ::blog-tags
  (fn [db]
    (:blog-tags db)))


(rf/reg-sub
  ::blog-post
  (fn [db]
    (:blog-post db)))


(rf/reg-sub
  ::blog-comments
  (fn [db]
    (:blog-comments db)))


(rf/reg-sub
  ::blog-comment-riddle
  (fn [db]
    (:blog-comment-riddle db)))


(rf/reg-sub
  ::admin-mode
  (fn [db]
    (:admin-mode db)))


(rf/reg-sub
  ::admin-post
  (fn [db]
    (:admin-post db)))


(rf/reg-sub
  ::view-mode
  (fn [db]
    (:view-mode db)))


(rf/reg-sub
  ::remote-error
  (fn [db]
    (:remote-error db)))
