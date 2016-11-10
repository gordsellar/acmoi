(ns acmoi.shared.spec
  #?(:clj (:require [clojure.spec :as s]
                    [clojure.spec.gen :as gen]
                    )
     :cljs (:require [cljs.spec :as s]))
  )

(def clearances
  "A map of clearance levels and their associated standings"
  {:IR {:income 100}
   :R {:income 1000}
   :O {:income 2000}
   :Y {:income 3000}
   :G {:income 10000}
   :B {:income 40000}
   :I {:income 100000}
   :V {:income 600000}
   ;; U gets 1e7 income
   :U {:income 10000000}
   }
  )
(s/def ::clearance
  (s/and keyword?
         #((-> clearances keys set) %)
         )
  )
(s/def ::income (s/and integer?
                       pos?))
(s/def ::fName (s/and string?
                      #(pos? (count %))
                      )
  )
(s/def ::zone (s/and string?
                     #(= 3 (count %))))
(s/def ::cloneNum (s/and integer?
                         pos?))
;; Citizen IDs start at 1
(s/def ::citizenId (s/and integer?
                          pos?))
(s/def ::associates (s/coll-of ::citizenId))
(s/def ::dead? boolean?)
(s/def ::gender #(#{:male :female} %))
;; Players may not yet have information on associates
(s/def ::citizenMap (s/keys :req-un [::citizenId ::clearance ::fName ::zone ::cloneNum ::gender]
                            :opt-un [::associates ::dead?]))
(s/def ::citizens (s/map-of ::citizenId ::citizenMap))
(s/def ::sector (s/keys :req-un [::zone ::citizens]))