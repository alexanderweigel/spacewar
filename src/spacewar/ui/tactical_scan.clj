(ns spacewar.ui.tactical-scan
  (:require [quil.core :as q]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :refer :all]
            [spacewar.vector :as v]
            [spacewar.vector :as vector]))

(defn- draw-background [state]
  (let [{:keys [w h]} state]
    (q/fill 0 0 0)
    (q/rect-mode :corner)
    (q/rect 0 0 w h)))

(defn- in-range [x y ship]
  (< (distance [x y] [(:x ship) (:y ship)]) (/ tactical-range 2)))

(defn- click->tactical [tactical-scan click]
  (let [{:keys [x y w h world]} tactical-scan
        ship (:ship world)
        center (vector/add [(/ w 2) (/ h 2)] [x y])
        scale [(/ tactical-range w) (/ tactical-range h)]
        click-delta (vector/subtract click center)
        tactical-click-delta (vector/multiply click-delta scale)]
    (vector/add tactical-click-delta [(:x ship) (:y ship)])))

(defn- present-objects [state objects]
  (let [{:keys [w h world]} state
        ship (:ship world)
        scale-x (/ w tactical-range)
        scale-y (/ h tactical-range)]
    (->> objects
         (filter #(in-range (:x %) (:y %) ship))
         (map #(assoc % :x (- (:x %) (:x ship))
                        :y (- (:y %) (:y ship))))
         (map #(assoc % :x (* (:x %) scale-x)
                        :y (* (:y %) scale-y))))))

(defn- draw-stars [state]
  (let [{:keys [w h world]} state
        stars (:stars world)
        presentable-stars (present-objects state stars)]
    (apply q/fill grey)
    (q/no-stroke)
    (q/ellipse-mode :center)
    (q/with-translation
      [(/ w 2) (/ h 2)]
      (doseq [{:keys [x y]} presentable-stars]
        (q/ellipse x y 4 4)))))

(defn- draw-klingons [state]
  (let [{:keys [w h world]} state
        klingons (:klingons world)
        presentable-klingons (present-objects state klingons)]
    (when klingons
      (apply q/fill black)
      (apply q/stroke klingon-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [klingon presentable-klingons]
        (let [{:keys [x y]} klingon]
          (q/with-translation
            [(+ x (/ w 2)) (+ y (/ h 2))]
            (q/line 0 0 10 -6)
            (q/line 10 -6 14 -3)
            (q/line 0 0 -10 -6)
            (q/line -10 -6 -14 -3)
            (q/ellipse 0 0 6 6)))))))

(defn- draw-ship [state]
  (let [{:keys [w h]} state
        heading (or (->> state :world :ship :heading) 0)
        velocity (or (->> state :world :ship :velocity) [0 0])
        [vx vy] (v/scale velocity-vector-scale velocity)
        radians (->radians heading)]
    (q/with-translation
      [(/ w 2) (/ h 2)]
      (apply q/stroke enterprise-vector-color)
      (q/stroke-weight 2)
      (q/line 0 0 vx vy)
      (q/with-rotation
        [radians]
        (apply q/stroke enterprise-color)
        (q/stroke-weight 2)
        (q/ellipse-mode :center)
        (apply q/fill black)
        (q/line -9 -9 0 0)
        (q/line -9 9 0 0)
        (q/ellipse 0 0 9 9)
        (q/line -5 9 -15 9)
        (q/line -5 -9 -15 -9)))))

(defn- draw-bases [state]
  (let [{:keys [w h bases]} state
        presentable-bases (present-objects state bases)]
    (q/no-fill)
    (apply q/stroke base-color)
    (q/stroke-weight 2)
    (q/ellipse-mode :center)
    (doseq [{:keys [x y]} presentable-bases]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (q/ellipse 0 0 12 12)
        (q/ellipse 0 0 20 20)
        (q/line 0 -6 0 6)
        (q/line -6 0 6 0)))))

(defn- phaser-intensity [range]
  (let [intensity (* 255 (- 1 (/ range phaser-range)))]
    [intensity intensity intensity]))

(defn- draw-torpedo-segment []
  (let [angle (rand 360)
        color (repeatedly 3 #(+ 128 (rand 127)))
        length (+ 5 (rand 5))
        radians (->radians angle)
        [tx ty] (vector/from-angular length radians)]
    (apply q/stroke color)
    (q/line 0 0 tx ty)))

(defn- draw-torpedo-shots [state]
  (let [{:keys [w h world]} state
        torpedo-shots (:torpedo-shots world)
        presentable-torpedo-shots (present-objects state torpedo-shots)]
    (doseq [{:keys [x y]} presentable-torpedo-shots]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (draw-torpedo-segment)
        (draw-torpedo-segment)
        (draw-torpedo-segment)))))

(defn- draw-kinetic-shots [state]
  (let [{:keys [w h world]} state
        kinetic-shots (:kinetic-shots world)
        presentable-kinetic-shots (present-objects state kinetic-shots)]
    (doseq [{:keys [x y]} presentable-kinetic-shots]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (q/ellipse-mode :center)
        (q/no-stroke)
        (apply q/fill kinetic-color)
        (q/ellipse 0 0 3 3)))))

(defn- draw-phaser-shots [state]
  (let [{:keys [w h world]} state
        phaser-shots (:phaser-shots world)
        presentable-phaser-shots (present-objects state phaser-shots)]
    (doseq [{:keys [x y bearing range]} presentable-phaser-shots]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (let [radians (->radians bearing)
              [sx sy] (vector/from-angular phaser-length radians)
              beam-color (phaser-intensity range)]
          (apply q/stroke beam-color)
          (q/line 0 0 sx sy))))))

(defn explosion-radius [age profile]
  (loop [profile profile radius 0 last-time 0]
    (let [{:keys [velocity until]} (first profile)]
      (cond (empty? profile)
            nil

            (> age until)
            (recur (rest profile)
                   (+ radius (* (- until last-time) velocity))
                   until)

            :else
            (+ radius (* velocity (- age last-time)))))))

(defn- draw-explosions [state]
  (let [{:keys [w h world]} state
        explosions (:explosions world)
        presentable-explosions (present-objects state explosions)]
    (doseq [{:keys [x y type age]} presentable-explosions]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (let [radius (explosion-radius age phaser-explosion-profile)]
          (apply q/fill grey)
          (q/ellipse-mode :center)
          (q/no-stroke)
          (q/ellipse 0 0 radius radius))))
    ))

(defn- draw-shots [state]
  (draw-phaser-shots state)
  (draw-torpedo-shots state)
  (draw-kinetic-shots state))

(deftype tactical-scan [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y]} state]
      (q/with-translation
        [x y]
        (draw-background state)
        (draw-stars state)
        (draw-shots state)
        (draw-klingons state)
        (draw-ship state)
        (draw-bases state)
        (draw-explosions state))))

  (setup [_]
    (tactical-scan. state))

  (update-state [_ world]
    (let [{:keys [x y w h]} state
          last-left-down (:left-down state)
          mx (q/mouse-x)
          my (q/mouse-y)
          mouse-in (inside-rect [x y w h] [mx my])
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          state (assoc state :mouse-in mouse-in :left-down left-down)
          event (if (and (not left-down) last-left-down mouse-in)
                  {:event :explosion-debug :position (click->tactical state [mx my])}
                  nil)]
      (p/pack-update (tactical-scan. (assoc state :world world)) event)))

  )