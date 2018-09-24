(ns spacewar.ui.control-panels.engine-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :refer :all]
            [spacewar.ui.widgets.lcars :refer :all]
            [spacewar.ui.widgets.button :refer :all]
            [spacewar.ui.widgets.direction-selector :refer :all]
            [spacewar.ui.widgets.slider :refer :all]
            [spacewar.ui.widgets.engage :refer :all]
            [quil.core :as q]))

(deftype engine-panel [state]
  p/Drawable
  (clone [_ clone-state]
    (engine-panel. clone-state))

  (draw [_]
    (let [{:keys [heading-label-x power-label-x label-y]} state]
      (draw-lcars state)
      (p/draw-elements state)
      (apply q/fill black)
      (q/text-align :center :bottom)
      (q/text-font (:lcars (q/state :fonts)) 14)
      (q/text "HEADING" heading-label-x label-y)
      (q/text "POWER" power-label-x label-y)))

  (setup [_]
    (let [{:keys [x y w h color button-color]} state
          button-w 150
          warp-y (+ y banner-width button-gap)
          impulse-y (+ warp-y button-h button-gap)
          dock-y (+ impulse-y button-h button-gap)
          direction-x (+ x button-w button-gap)
          direction-y (+ y banner-width button-gap)
          direction-diameter (- h banner-width)
          power-x (+ direction-x direction-diameter button-gap)
          power-y direction-y
          power-w slider-width
          power-h (- h banner-width)
          engage-x (- (+ x w) stringer-width button-gap engage-width)
          engage-y direction-y
          engage-w engage-width
          engage-h (- h banner-width)

          heading-label-x (+ direction-x (/ direction-diameter 2))
          power-label-x (+ power-x (/ power-w 2))
          label-y (+ y banner-width)]

      (engine-panel.
        (assoc state
          :heading-label-x heading-label-x
          :power-label-x power-label-x
          :label-y label-y
          :warp (p/setup
                  (->button
                    {:x x
                     :y warp-y
                     :w button-w
                     :h button-h
                     :name "WARP"
                     :color button-color
                     :left-up-event {:event :select-warp}}))
          :impulse (p/setup
                     (->button
                       {:x x
                        :y impulse-y
                        :w button-w
                        :h button-h
                        :name "IMPULSE"
                        :color button-color
                        :left-up-event {:event :select-impulse}}))
          :dock (p/setup
                  (->button
                    {:x x
                     :y dock-y
                     :w button-w
                     :h button-h
                     :name "DOCK"
                     :color button-color
                     :left-up-event {:event :select-dock}}))

          :direction-selector (p/setup
                                (->direction-selector
                                  {:x direction-x
                                   :y direction-y
                                   :diameter direction-diameter
                                   :direction 0
                                   :color color
                                   :left-up-event {:event :engine-direction}}))
          :power-slider (p/setup
                          (->slider
                            {:x power-x
                             :y power-y
                             :w power-w
                             :h power-h
                             :color color
                             :thumb-color button-color
                             :min-val 0
                             :max-val 100
                             :value 50
                             :left-up-event {:event :engine-power}}))

          :engage (p/setup
                    (->engage
                      {:x engage-x
                       :y engage-y
                       :w engage-w
                       :h engage-h
                       :name "ENGAGE"
                       :color color
                       :activation-color button-color
                       :left-up-event {:event :engine-engage}}))

          :elements [:warp :impulse :dock :direction-selector :power-slider :engage]))))

  (update-state [_ commands-and-state]
    (let [commands (:commands commands-and-state)
          global-state (:global-state commands-and-state)
          heading (->> global-state :ship :heading)
          direction-command (p/get-command :set-engine-direction commands)
          power-command (p/get-command :set-engine-power commands)
          commanded-state (cond
                            (some? direction-command)
                            (p/assoc-element state :direction-selector :direction (:angle direction-command))

                            (some? power-command)
                            (p/assoc-element state :power-slider :value (:power power-command))

                            :else state)
          commanded-state (p/assoc-element commanded-state :direction-selector :pointer2 heading)
          [new-state events] (p/update-elements commanded-state commands-and-state)]
      (p/pack-update
        (engine-panel. new-state)
        events))))