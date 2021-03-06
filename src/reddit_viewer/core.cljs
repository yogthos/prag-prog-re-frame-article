(ns reddit-viewer.core
  (:require
    [reagent.core :as r]
    [reddit-viewer.chart :as chart]
    [reddit-viewer.events]
    [re-frame.core :as rf]))

;; -------------------------
;; Views

(defn display-post [{:keys [permalink num_comments subreddit title score url]}]
  [:div.card.m-2
   [:div.card-block
    [:h4.card-title
     [:a {:href (str "http://reddit.com" permalink)} title " "]]
    [:div [:span.badge.badge-info
           {:color "info"}
           subreddit " score " score " / comments " num_comments]]
    [:img {:width "300px" :src url}]]])

(defn display-posts [posts]
  (when-not (empty? posts)
    [:div
     (for [posts-row (partition-all 3 posts)]
       ^{:key posts-row}
       [:div.row
        (for [post posts-row]
          ^{:key post}
          [:div.col-4 [display-post post]])])]))

(defn sort-posts [title sort-key]
  [:button.btn.btn-secondary
   {:on-click #(rf/dispatch [:sort-posts sort-key])}
   (str "sort posts by " title)])

(defn navitem [title page id]
  [:li.nav-item
   {:class-name (when (= id page) "active")}
   [:a.nav-link
    {:href     "#"
     :on-click #(rf/dispatch [:select-page id])}
    title]])

(defn navbar [page]
  [:nav.navbar.navbar-toggleable-md.navbar-light.bg-faded
   [:ul.navbar-nav.mr-auto.nav
    {:className "navbar-nav mr-auto"}
    [navitem "Posts" page :posts]
    [navitem "Chart" page :chart]]])

(defn home-page []
  (let [page @(rf/subscribe [:page])]
    [:div
     [navbar page]
     [:div.card>div.card-block
      [:div.btn-group
       [sort-posts "score" :score]
       [sort-posts "comments" :num_comments]]
      (case page
        :chart [chart/chart-posts]
        :posts [display-posts @(rf/subscribe [:posts])])]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root)
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:load-posts]))