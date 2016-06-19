(ns battleshot.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]]))

(def remote (js/require "remote"))
(def dialog (.require remote "dialog"))
(def fs (js/require "fs"))

(def image-atom (reagent/atom {}))

(defn is-jpeg [f]
  (let [f-lower (.toLowerCase f)]
    (or (.endsWith f-lower ".jpg")
        (.endsWith f-lower ".jpeg"))))

(defn listen [out-chan el type]
  (events/listen el type (fn [e] (put! out-chan e))))

(defn setup-rating! [images]
  (let [click-chan (chan)
        el-first (dom/getElement "first")
        el-second (dom/getElement "second")
        image-count (count images)]
    (swap! image-atom #(apply sorted-map (interleave (range (count images)) images)))
    (listen click-chan el-first "click")
    (listen click-chan el-second "click")
    (go-loop [[[first-rank first-image] [second-rank second-image]] (shuffle @image-atom)]
      (set! (.-src el-first) first-image)
      (set! (.-src el-second) second-image)
      (let [msg (<! click-chan)]
        (swap! image-atom (fn [m] (assoc m first-rank second-image second-rank first-image)))
        (recur (shuffle @image-atom))))))

(defn image-page
  [images]
  (reagent/create-class
   {:component-did-mount (fn [] (setup-rating! (vec images)))

    :reagent-render (fn [_]
                      [:div {:id :image-container}
                       [:ul {:id :image-list}
                        (for [[k v] @image-atom]
                          ^{:key k} [:li v])]
                       [:img {:id :first}]
                       [:img {:id :second}]])}))

(defn load-folder []
  (let [folder (.showOpenDialog dialog (.getCurrentWindow remote) #js {:properties #js ["openDirectory"]})
        files (->> (.readdirSync fs (get folder 0))
                   (mapv #(str folder "/" %)))
        jpegs (filter is-jpeg files)]
    (reagent/render [image-page jpegs] (.getElementById js/document "app"))))

(defn main-page
  []
  [:div [:button {:on-click load-folder} "load images"]])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (mount-root))
