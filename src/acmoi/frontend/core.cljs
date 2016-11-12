(ns acmoi.frontend.core
  (:require [taoensso.timbre :as log]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST] :as ajax]
            [cljs.js :as cjs]
            [acmoi.shared.spec :as ss]
            ))

(enable-console-print!)

(println "This text is printed from src/acmoi/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

;; TODO change back to defonce when required
(def app-state (atom
                 {:citizens {}
                  :poi (repeatedly 30 #(inc (rand-int 1000)))
                  }
                 )
  )
;(def userInfo (atom {:userCitizen 1 :userKey "tempApiKey"}))
(def userInfo (atom nil))

(def ^:private colour-styles
  "Maps clearances to foreground and background colours"
  {:IR {}
   :R {:background-color "DarkRed"}
   :O {:background-color "#DF7C00"}
   :Y {:background-color "GoldenRod"}
   :G {:background-color "Green"}
   :B {:background-color "MediumBlue"}
   :I {:background-color "Indigo"}
   :V {:background-color "DarkViolet"}
   :U {:color "Black" :background-color "White"}
   }
  )

(defn- get-citizen-basic
  "Gets basic citizen info"
  [citizenId]
  (GET "/api/citizen/basic/"
       {:params {:citizenId citizenId
                 :userKey (:userKey @userInfo)}
        :response-format (ajax/json-response-format {:keywords? true})
        :handler (fn [m]
                   (swap! app-state assoc-in [:citizens citizenId] m)
                   (log/info "app-state is now:" @app-state)
                   )
        }
       )
  )
(defn- get-citizen-associates
  "Gets the associates of a citizen"
  [citizenId]
  (GET "/api/citizen/associates/"
       {:params {:citizenId citizenId
                 :userKey (:userKey @userInfo)}
        :response-format (ajax/json-response-format {:keywords? true})
        :handler #(do (log/trace "Requesting associates of citizen" citizenId)
                      (swap! app-state assoc-in [:citizens citizenId :associates] %)
                      )
        }
       )
  )

(defn- print-person-name
  "Prints a citizen's name from a map"
  [{:keys [fName zone clearance cloneNum]}]
  (if fName
    (str fName "-"
         (if (= :IR clearance)
           ""
           (str (name clearance) "-")
           )
         zone "-"
         cloneNum
         )
    "Loading"
    )
  )

(defn create-citizen-box
  [n]
  (let [expand (atom false)]
    (fn []
      (let [cmap (get-in @app-state [:citizens n])]
        (if (not (:citizenId cmap))
            (get-citizen-basic n)
            nil
            )
        [:div {:style (-> {:margin "2px"}
                          (#(if @expand
                              (merge % {:border "1px solid white"})
                              %))
                          (merge (get colour-styles (keyword (:clearance cmap)) {:color "Red"}))
                          )
               :onClick #(if (not (:citizenId cmap)) (get-citizen-basic n) nil)
               }
         (if @expand
           [:span
            [:span {:onClick #(do (js/console.log (str "Clicked on person:" n))
                                  (swap! expand not)
                                  false)
                    }
             "Name: " (print-person-name cmap)] [:br]
            "Id Number:" n [:br]
            (if (:associates cmap)
              ;; Associates known
              [:span "Known associates:" [:br]
               (for [p (:associates cmap)]
                 [create-citizen-box p])
               ]
              ;; Associates unknown
              [:span {:onClick #(get-citizen-associates n)}
               "##Get Associates##"
               ]
              )

              ]
           [:span {:onClick #(do (js/console.log (str "Clicked on person:" n))
                                 (swap! expand not)
                                 false)
                   }
            (print-person-name cmap)]
           )
         ]
        )
      )
    )
  )
(defn main-page []
  (let [expand (atom false)]
    (fn []
      [:div
       [:table
        [:tr
         [:td {:colSpan 3
               :border "1px dotted White"
               }
          "Testing"
          ]
         ]
        [:tr
         ;; Left column. Top part: objectives. Bottom part: citizen information
         [:td {:style {:border "1px dotted White"}}
          ;; Creates a citizen box for each person of information
          [:div (doall (for
                         [n (-> @app-state :poi)]
                         ^{:key n}
                         [create-citizen-box n]
                         ))]
          ]
         ;; Middle column, surveillance information
         [:td {:style {:border "1px dotted White"
                       }}
          ]
         ;; Right column, forms
         [:td {:style {:border "1px dotted White"
                       }}
          ]
         ]
        ]
       ]
      )
    )
  )
(defn login-page
  "User login form"
  []
  (fn []
    [:div
     "Testing Login Form"
     [:span {:onClick #(reset! userInfo {:userCitizen 1 :userKey "tempApiKey"})}
      "##LOG IN##"
      ]
     ]
    )
  )
(defn front
  "Render whether it needs the login form or the main page"
  []
  (fn []
    (if (:userKey @userInfo)
      [main-page]
      [login-page]
      )
    )
  )


(reagent/render-component [front]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
