(ns reddit-viewer.events
  (:require
    [ajax.core :as ajax]
    [re-frame.core :as rf]))

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {}))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(rf/reg-event-db
  :set-posts
  (fn [db [_ posts]]
    (assoc db :posts
              (->> (get-in posts [:data :children])
                   (map :data)
                   (find-posts-with-preview)))))

(rf/reg-event-fx
  :load-posts
  (fn [_ _]
    (ajax/GET "http://www.reddit.com/r/Catloaf.json?sort=new&limit=50"
              {:handler         #(rf/dispatch [:set-posts %])
               :response-format :json
               :keywords?       true})
    nil))

(rf/reg-event-db
  :sort-posts
  (fn [db [_ sort-key]]
    (update db :posts (partial sort-by sort-key >))))

(rf/reg-sub
  :posts
  (fn [{:keys [posts]} _]
    posts))

(rf/reg-event-db
  :select-page
  (fn [db [_ page]]
    (assoc db :page page)))

(rf/reg-sub
  :page
  (fn [db _]
    (:page db :posts)))