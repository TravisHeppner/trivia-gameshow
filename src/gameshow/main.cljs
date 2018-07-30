(ns ^:figwheel-always gameshow.main
  (:require
    [gameshow.ainit]
    [gameshow.view :as view]
    [reagent.core :as reagent]))

(reagent/render-component
  [view/main]
  (. js/document (getElementById "app")))
