# Re-framing your Expectations

If you've been keeping up with JavaScript front-end development, then you've probably heard of React by now. React is a user interface library that was open sourced by Facebook, and quickly gained popularity among front-end developers.

One of the key insights behind React was that DOM manipulation is computationally expensive, and tends to be a performance bottleneck for many applications. React addresses this problem by keeping a Virtual DOM, and computing changes against it. Once the final set of changes is produced it is then applied to the actual DOM.

This approach unburdens the developers from having to manually optimize DOM updates in their applications. The developer can now work from the perspective that the whole page will be repainted any time a change occurs, while React figures out the minimal set of DOM elements that actually need to be updated.

React approach to UI development happens to be a perfect fit for a functional language. This article will give you a taste of developing an application using ClojureScript [re-frame](https://github.com/Day8/re-frame) framework built on top of React. We will introduce basic concepts behind re-frame, and illustrate JavaScript interop using the [Chart.js](http://www.chartjs.org/) library.

## Prerequisites

This article requires familiarity with Clojure syntax and the Reagent library to follow along. Please take a look at the [Clojure Distilled](https://yogthos.github.io/ClojureDistilled.html) guide for a quick Clojure refresher, and the [Reagent documentation](http://reagent-project.github.io/) if you're not already familiar with them.

## Before you start

Please make sure that you have a copy of the JDK and Leiningen build tool setup to follow along with the material. You can follow installation instructions in the links below:

* [JDK 1.8+](http://www.azul.com/downloads/zulu/)
* [Leiningen](https://leiningen.org/)

It is recommended that you use Chrome browser to follow along.

### Creating and running the project

We'll be building a small application that will load JSON data from the Reddit API, and visualize it. The first step is to create a new project for the application. This is accomplished by running the following command:

    lein new reagent-frontend reddit-viewer

The above command will generate a new project using the [Reagent front-end template](https://github.com/reagent-project/reagent-frontend-template/) in the folder called `reddit-viewer`. Let's navigate to the project folder that was just created and see what was generated for us:

```
├── LICENSE
├── README.md
├── project.clj
├── env
│   ├── dev
│   │   └── cljs
│   │       └── reddit_viewer
│   │           └── dev.cljs
│   └── prod
│       └── cljs
│           └── reddit_viewer
│               └── prod.cljs
├── public
│   ├── css
│   │   └── site.css
│   └── index.html
└── src
    └── reddit_viewer
        └── core.cljs
```

The generated project contains a few folders and files.

* The `env` directory contains environment specific source code for bootstrapping the application for development and production.
* The `public` folder contains assets that will be served by HTTP server during development.
* The `src` folder contains the source code for the project.

## Working with the project

Leiningen manages project lifecycle using the `project.clj` file. This file contains information about building the project, managing dependencies, running tests, and packaging code for production use. We'll update project dependencies in `project.clj` to look as follows:

```clojure
 :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.854"]
                 [reagent "0.7.0"]
                 [re-frame "0.9.4"]
                 [cljsjs/chartjs "2.5.0-0"]
                 [cljs-ajax "0.6.0"]]
```

Next, let's replace the generated CSS link with the Bootstrap CSS in the `public/index.html` file:

```xml
<head>
    <meta charset="utf-8">
    <meta content="width=device-width, initial-scale=1" name="viewport">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/css/bootstrap.min.css">
</head>
```

We're now ready to start the project in development mode by running the following command:

    lein figwheel

Leiningen will download the dependencies and start compiling the project, this can take a minute first time around.
Once the project compilation finishes, a browser window will open at [http://localhost:3449/index.html](http://localhost:3449/index.html).

## Editing the project

Now that we have the project running, let's see how we can add some functionality to it.
We'll open up the `reddit_viewer/core.cljs` file to see what it's doing in order to render the page.

```clojure
(ns reddit-viewer.core
    (:require
      [reagent.core :as r]))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to Reagent"]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
```

The top section of the file contains a namespace declaration. The namespace requires the `reagent.core` namespace that's
used to create the UI.

The `home-page` function creates a Reagent component. The component contains a `div` with an `h2` tag inside it.

Reagent uses Clojure literal notation for vectors and maps to represent HTML. The tag is defined using a vector, where the first element is the keyword representing the tag name, followed by an optional map of attributes, and the tag content.

For example, `[:div [:h2 "Welcome to Reagent"]` maps to `<div><h2>Welcome to Reagent</h2></div>`. If we wanted to add `id` and `class` to the `div`, we could do that as follows: `[:div {:id "foo" :class "bar baz"} ...]`.

Since setting the `id` and `class` attributes is a very common operation, Reagent provides a shortcut for doing that using syntax similar to CSS selectors: `[:div#foo.bar.baz ...]`.

This component is rendered inside the DOM element with the `id` equal to `app`. This element is defined in the `public/index.html` file
by the `mount-root` function.

Finally, we have the `init!` function that serves as the entry point for the application.

### Re-frame concepts

Re-frame uses a reactive atom to represent the state of the application. This atom is used internally by re-frame, and we don't interact with it directly. Instead, we use dispatchers to update the state of the atom, and subscriptions to observe it.

Essentially, re-frame follows the MVC approach to structuring the UI code. The model is modified using dispatchers, and the view observes it via subscriptions. Let's take a look at how this works in practice.

We will `dispatch` events whenever we wish to update the state, and `subscribe` to changes in our components that observe it. Let's take a look at what event handlers and subscriptions look like.

#### re-frame event handlers

Event handlers are defined using the `re-frame.core/reg-event-db` function. The function accepts a keyword used to uniquely identify the event, and a function that will be triggered when the event is dispatched. Let's take a look at an example handler:

```clojure
(re-frame.core/reg-event-db
 :set-value
 (fn [db [event-id value]]
   (assoc db :value value)))
```

We now have an handler associated with the `:set-value` event. The event handling function accepts two arguments. The first argument is the current state of the re-frame atom, and the second is the vector of arguments passed to the event. The first element of the arguments vector will be the event id, in this case `:set-value`, followed by zero or more optional arguments.

Now that the event has been defined, let's take a look at how we dispatch it. This is done using the `re-frame.core/dispatch` function:

```clojure
(re-frame.core/dispatch [:set-value "some value"])
```

The above code will trigger the `:set-value` event, and the vector `[:set-value "some value"]` will be passed to the event handler function.

 The function will associate the `:value` key in the `db` with the value that was passed in. In our case, the value will become the string `"some value"`.

Now that we've seen how to create an event handler to update the re-frame database, let's take a look at how we can subscribe to views inside it.

#### re-frame subscriptions

Subscriptions are created using the `re-frame.core/reg-sub` function. This function has similar semantics to the `reg-event-db` function. Let's look at a concrete example of a subscription to a key in the re-frame atom below:

```clojure
(re-frame.core/reg-sub
 :view-key
 (fn [db [event-id k]]
   (get db k)))
```

Once again, the function accepts an identifier followed by the handler function. The handler function accepts the current state of the atom, followed by a vector of arguments.

To create a subscription to the `:value` key we set earlier, we use the `re-frame.core/subscribe` function:

```clojure
(re-frame.core/subscribe [:view-key :value])
```

The subscription returns a Reagent reaction that contains the computation for the subscription. This reaction will only be evaluated when the state of the database changes. In order to get the value from the reaction, we need to dereference it as follows:

```clojure
[:p @(re-frame.core/subscribe [:view-key :value])]
```

That's all we need to know about re-frame to update our application. We'rew now ready to take a look at how we can update the project to use it.

## Task 1: Loading data using Ajax and viewing it

First thing we'll need to do is to load the data from the remote API via an AJAX call. This data will be populated in the re-frame database once it's returned by the server.

We'll call the `http://www.reddit.com/r/Catloaf.json?sort=new&limit=50` URL to load the data. The URL will return the data that has the following structure:

```
{:data {:children [{:data {...}} ...]}}
```

The top level data structure is a map that contains a key called `:data`, this key points to a map that contains a key called `:children`. Finally, the `:children` key points to a collection of maps representing the posts. Each map, in turn, has a key called `:data` that contains the data for the specific post.

Let's start by creating a new file called `src/reddit_viewer/events.cljs`. This will be the namespace where we'll place all our re-frame events.

We'll require the `ajax.core` and `re-frame.core` namespaces in the declaration of the `reddit-viewer.events` namespace:

```clojure
(ns reddit-viewer.events
  (:require
    [ajax.core :as ajax]
    [re-frame.core :as rf]))
```

The first event we have to create is the one that will initialize the database. This event will look as follows:

```clojure
(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {}))
```

The event handler function will simply return an empty map as its return value giving us an empty database.

The next event that we'll create will be called `:set-posts` and it will be responsible for populating the posts data:

```clojure
(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(rf/reg-event-db
  :set-posts
  (fn [db [_ posts]]
    (assoc db :posts
              (->> (get-in posts [:data :children])
                   (map :data)
                   (find-posts-with-preview)))))
```

The event handler will traverse the data structure to find the maps containing the information about the posts, and filter the posts that contain a `:post_hint` key indicating that the post contains an image.

The event will be called by the `:load-posts` event that will make an AJAX call to fetch the data:

```clojure
(rf/reg-event-fx
  :load-posts
  (fn [_ _]
    (ajax/GET "http://www.reddit.com/r/Catloaf.json?sort=new&limit=50"
              {:handler         #(rf/dispatch [:set-posts %])
               :response-format :json
               :keywords?       true})
    nil))
```

Notice that we're using `reg-event-fx` function instead of the `reg-event-db`. This is done to indicate that we're creating a side effect and we're not affecting the state of the database at this time. A `nil` value is returned by this event. The callback function associated with the `:handler` key will dispatch the `:set-posts` event asynchronously when the data is received.

With these events in place, we now have the ability to load remote data into our application. We'll now need to add a subscription in order to view the data. Let's create the following subscription:

```clojure
(rf/reg-sub
  :posts
  (fn [{:keys [posts]} _]
    posts))
```

Now, let's come back to the `reddit-viewer.core` namespace and update it to allows us to view the posts in the UI.

We'll update the namespace declaration to require `re-frame.core` to require `reddit-viewer.events`, and `re-frame.core` namespaces

```clojure
(ns reddit-viewer.core
    (:require
      [reagent.core :as r]
      [reddit-viewer.events]
      [re-frame.core :as rf]))
```

We can now update `home-page` function to display the value of the `:posts` subscription:

```clojure
(ns reddit-viewer.core
    (:require
      [reagent.core :as r]
      [reddit-viewer.events]
      [re-frame.core :as rf]))

;; -------------------------
;; Views

(defn home-page []
  [:div (str @(rf/subscribe [:posts]))])
```

Of course, there aren't any posts loaded yet. Let's go to the terminal where we ran the `lein figwheel` command. It will have a REPL that allows us to modify the state of the application. We'll run the following command there to switch to the `reddit-viewer.core` namespace:

```clojure
(in-ns 'reddit-viewer.core)
```

Next, we'll disptach the event that initializes the re-frame database:

```clojure
(rf/dispatch-sync [:initialize-db])
```

Re-frame events dispatched using the `dispatch` function happen asynchronously. However, we want to make sure that the database is initialized before we start using it, there fore this event is dispatched using the `dispatch-sync` function.

Finally, we'll dispatch the `:load-posts` event:

```clojure
(rf/dispatch-sync [:load-posts])
```

Once the event runs, we should see the string representation of the posts data rendered on the page.

Now that we've confirmed that we're able to load the data successfully, let's update the `'init!` function to run these events when the page loads:

```clojure
(defn init! []
  (mount-root)
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:load-posts]))
```

If we reload the page in the browser, we should see the posts printed on the screen. We're now ready to take a look at creating some UI components to render the posts in a better way.

## Task 2: Creating UI components

We'll start by writing a function that will render a single post:

```clojure
(defn display-post [{:keys [permalink num_comments subreddit title score url]}]
  [:div.card.m-2
   [:div.card-block
    [:h4.card-title
     [:a {:href (str "http://reddit.com" permalink)} title " "]]
    [:div [:span.badge.badge-info
           {:color "info"}
           subreddit " score " score " / comments " num_comments]]
    [:img {:width "300px" :src url}]]])
```

The function accepts a map representing the post, and extracts the relevant keys from it. These are used to create the HTML elements for displaying the post. We can test that this function is working as expected by rendering one of the posts that we've fetched earlier:


```clojure
(defn home-page []
  [:div.card>div.card-block
   [display-post (first @(rf/subscribe [:posts]))]])
```

Now we can write a function that takes a collection of posts and uses the `display-post` component to render each one:

```clojure
(defn display-posts [posts]
  (when-not (empty? posts)
    [:div
     (for [posts-row (partition-all 3 posts)]
       ^{:key posts-row}
       [:div.row
        (for [post posts-row]
          ^{:key post}
          [:div.col-4 [display-post post]])])]))
```

The `display-posts` function accepts a collection of maps representing the posts, and renders them using the Bootstrap grid layout. The posts are partitioned into groups of three to represent individual rows.

You'll notice that metadata is used to provide a unique key to each row and post. This is an optimization that allows React to track changes in each individual component when repainting. When we omit this metadata, all the components in the collection must be repainted whenever any one component changes.

## Task 3: Manipulating data

Now that we have the posts rendering nicely on the page, let's take a look at what happens when we change the data that's being rendered. We'll add an event to sort the posts and hook it up to buttons that will sort posts by their score and the number of comments.

Let's navigate back to the `reddit-viewer.events` namespace, and add the following event there:

```clojure
(rf/reg-event-db
  :sort-posts
  (fn [db [_ sort-key]]
    (update db :posts (partial sort-by sort-key >))))
```

The event accepts a sort-key as its parameter and updates the `:posts` key in the database with the sorted result. We can now navigate back to the `reddit-viewer.core` namespace and add the buttons for sorting that will dispatch the event.

We'll start by creating a component that generates a sort button. The component will accept a `title` and a `sort-key` as its parameters. When the button is clicked, it will dispatch the `:sort-posts` event we just defined with the provided `sort-key`.

```clojure
(defn sort-posts [title sort-key]
  [:button.btn.btn-secondary
   {:on-click #(rf/dispatch [:sort-posts sort-key])}
   (str "sort posts by " title)])
```

Let's update the `home-page` function with the sort buttons:

```clojure
(defn home-page []
  [:div.card>div.card-block
   [:div.btn-group
    [sort-posts "score" :score]
    [sort-posts "comments" :num_comments]]
   [display-posts @(rf/subscribe [:posts])]])
```

We should now see two buttons at the top of the page, and clicking them will dispatch the sort event. This event will cause the data to be resorted, and trigger the UI to update.

## Task 4: Charting the data

We'll now take a look at charting the posts using the Chart.js library. This task will illustrate how we can leverage existing JavaScript libraries in re-frame applications.

Let's create a new namespace called `reddit-viewer.chart` with the following namespace declaration:


```clojure
(ns reddit-viewer.chart
  (:require
    [cljsjs.chartjs]
    [reagent.core :as r]
    [re-frame.core :as rf]))
```

Requiring the `cljsjs.chartjs` namespace loads the ClojureScript adapter for the Chart.js library we included earlier in our dependencies in the `project.clj` file.

JavaScript global variables are available under the `js` namespace, and a new `Chart` object can be created by calling `js/Chart.` and passing it the target DOM node followed by a JSON options map:

```clojure
(defn render-data [node data]
  (js/Chart.
    node
    (clj->js
      {:type    "bar"
       :data    {:labels   (map :title data)
                 :datasets [{:label "votes"
                             :data  (map :score data)}]}
       :options {:scales {:xAxes [{:display false}]}}})))
```

This is equivalent to writing the following JavaScript code:

```
new Chart(node
          {type: "bar",
           data: {
                  labels: data.map(function(x) {return x.title}),
                  datasets:
                  [{
                    label: "votes",
                    data: data.map(function(x) {return x.ups})
                   }]
                  },
           options: {
                     scales: {xAxes: [{display: false}]}
                    }
           });
```

We now have a function that can render the chart, but we still need to instantiate a DOM node for it to use. Since our application is backed by React, we have to hook into its component lifecycle to ensure that the DOM element is available before we attempt to use it.

So far we've been writing components as functions that return HTML elements. However, these functions only represent the render method of a React class.

In order to get access to the DOM we have to implement other lifecycle functions that get called when the component is mounted, updated, and unmounted. This is achieved by calling the `create-class` function:

```clojure
(defn chart-posts []
  (let [chart (atom nil)]
    (r/create-class
      {:component-did-mount    (render-chart chart)
       :component-did-update   (render-chart chart)
       :component-will-unmount (fn [_] (destroy-chart chart))
       :render                 (fn [] (when @(rf/subscribe [:posts]) [:canvas]))})))
```

The function accepts a map keyed on the lifecycle events. Whenever each event occurs, the associated function will be called.

We'll track the state of the chart using an atom. This will be necessary because we have to destroy the existing chart when component is unmounted.

You can see that the `:render` key points to a function that will return the `:canvas` element when data is available.

The `:component-did-mount` and `:component-did-update` keys point to the `render-chart` function that w'll write next:

```clojure
(defn render-chart [chart]
  (fn [component]
    (when-let [posts @(rf/subscribe [:posts])]
      (destroy-chart chart)
      (reset! chart (render-data (r/dom-node component) posts)))))
```

This function is a closure that returns a function that will receive the React component. The inner function will check if there's any data available, and if so, then it will grab the mounted DOM node by calling `r/dom-node` on the `component`. It will attempt to clear the existing chart by calling the `destroy-chart` function, and then create a new chart by calling the `render-data` function we wrote earlier.

Finally, we'll implement the `destroy-chart` function as follows:

```clojure
(defn destroy-chart [chart]
  (when @chart
    (.destroy @chart)
    (reset! chart nil)))
```

This function will check whether there's an existing chart present and call its `destroy` method. It will then reset the `chart` atom to a `nil` value.

With that in place, we can navigate back to the `reddit-viewer.core` namespace, and require the `reddit-viewer.chart` namespace there:

```clojure
(ns reddit-viewer.core
  (:require
    [reagent.core :as r]
    [reddit-viewer.chart :as chart]
    [reddit-viewer.events]
    [re-frame.core :as rf]))
```


We'll now update the `home-page` component to display the chart:

```clojure
(defn home-page []
  [:div.card>div.card-block
   [:div.btn-group
    [sort-posts "score" :score]
    [sort-posts "comments" :num_comments]]
   [chart/chart-posts]
   [display-posts @(rf/subscribe [:posts])]])
```

We can see that the chart is now being rendered above the posts, and that it updates when we change the sort order of the posts data.

## Task 5: Adding navigation

As a last touch, let's add a navbar for the application, and create separate pages for viewing posts and charting them. We'll create a key in the database to track the current page. This key will be set using a new event called `:select-page`, and the selected page will be observed using the `:page` subscription. We'll add these in the `reddit-viewer.events` namespace:

 ```clojure
(rf/reg-event-db
  :select-page
  (fn [db [_ page]]
    (assoc db :page page)))

(rf/reg-sub
  :page
  (fn [db _]
    (:page db :posts)))
```

The subscription will the value of the `:page` key, and a default value of `:posts` when the key is not set. Next, we'll go back to the `reddit-viwer.core` namespace, where we'll add the function to render the navbar and navitem components:

```
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
```

Finally, we'll update the `home-page` to subscribe to the selected page, and render the appropriate UI component:

```clojure
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
```

## Compiling for release

So far we've been working with ClojureScript in development mode. This compilation method allows for fast incremental compilation and reloading. However, it generates very large JavaScript files.

To use our app in production we'll want to use the advanced compilation method that will produce optimized JavaScript. This is accomplished by running the following command:

    lein package

This will produce a single minified JavaScript file called `public/js/app.js` that's ready for production use.


## Conclusion

I hope this article provided you with a bit of insight into how ClojureScript applications are developed using re-frame. The main advantage of the re-frame approach is that it naturally separates business logic from the UI components. If you look back at the project, you'll notice that all the business logic lives in the `reddit-viewer.events` namespace, and the UI component functions in the `reddit-viewer.core` simply dispatch events and subscribe to views in the re-frame database. This approach provides us with a clean and scalable model for writing large single page applications.

Since re-frame components don't have any internal state, it's easy to reason about their lifecycle. The only time we had to explicitly manage React lifecycle callbacks was when we were using an external library that needed access to the DOM.

My experience working with Re-frame is that the resulting code is optimized for readability without sacrificing efficiency. This results in much simpler application structure that results in faster development, and improved ease of maintenance compared to traditional React applications.


## Libraries used in the project

* [Chart.js](http://www.chartjs.org/) - used to generate the bar chart
* [cljs-ajax](https://github.com/JulianBirch/cljs-ajax) - used to fetch data from Reddit
* [Reagent](reagent-project.github.io) - ClojureScript interface for React
* [re-frame](https://github.com/Day8/re-frame) - Reagent framework for writing SPAs

