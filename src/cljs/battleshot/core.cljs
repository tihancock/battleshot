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

(defn is-jpeg [f]
  (let [f-lower (.toLowerCase f)]
    (or (.endsWith f-lower ".jpg")
        (.endsWith f-lower ".jpeg"))))

(defn listen [out-chan el msg type]
  (events/listen el type (fn [_] (put! out-chan msg))))

(defn setup-rating! [images]
  (let [click-chan (chan)
        el-first (dom/getElement "first")
        el-second (dom/getElement "second")]
    (listen click-chan el-first :first "click")
    (listen click-chan el-second :second "click")
    (go-loop [[first-image second-image] (shuffle images)
              comparisons []]
      (set! (.-src el-first) first-image)
      (set! (.-src el-second) second-image)
      (let [msg (<! click-chan)
            comparison (case msg
                         :first  [first-image second-image]
                         :second [second-image first-image]
                         (throw (js/Error. "Neither first nor second image selected!")))]
        (recur (shuffle images) (conj comparisons comparison))))))

(defn image-page
  [images]
  (reagent/create-class
   {:component-did-mount (fn [] (setup-rating! (vec images)))

    :reagent-render (fn [_]
                      [:div {:id :image-container}
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
