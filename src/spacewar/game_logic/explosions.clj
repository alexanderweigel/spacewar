(ns spacewar.game-logic.explosions
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.game-logic.config :refer :all]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]
    [clojure.spec.alpha :as s]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::type #{:phaser :torpedo :kinetic :klingon})
(s/def ::age number?)
(s/def ::velocity number?)
(s/def ::direction number?)
(s/def ::fragments (s/coll-of (s/keys :req-un [::x ::y ::velocity ::direction])))
(s/def ::explosion (s/keys :req-un [::x ::y ::type ::age ::fragments]))

(defn make-fragments [n explosion velocity]
  (let [{:keys [x y]} explosion]
    (repeatedly n
                #(identity {:x x :y y
                            :velocity (* (+ 0.8 (rand 0.2)) velocity)
                            :direction (rand 360)}))))

(defn- active-explosion [explosion]
  (let [{:keys [age type]} explosion
        profile (type explosion-profiles)
        duration (:duration profile)]
    (> duration age)))

(defn update-explosions [ms world]
  (let [explosions (:explosions world)
        explosions (map #(update % :age + ms) explosions)
        explosions (filter active-explosion explosions)]
    (assoc world :explosions (doall explosions))))

(defn ->explosion [explosion-type {:keys [x y] :as explosion}]
  (let [profile (explosion-type explosion-profiles)]
    {:x x :y y
     :age 0 :type explosion-type
     :fragments (make-fragments (:fragments profile) explosion (:fragment-velocity profile))})
  )