(ns acmoi.frontend.core
  (:require [taoensso.timbre :as log]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST] :as ajax]
            [acmoi.shared.spec :as ss]
            ))

(enable-console-print!)

(println "This text is printed from src/acmoi/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

;; TODO change back to defonce when required
(defonce app-state (atom
                 {:citizens {}
                  :poi (repeatedly 5 #(inc (rand-int 1000)))
                  }
                 )
  )
(defonce userInfo (atom nil))
(defonce systemInfo
  (atom {:context (-> js/window
                      .-location
                      ;; This gets /context/index.html
                      .-pathname
                      ;; Remove the ending
                      (clojure.string/replace "/index.html" "")
                      )
         :buttonSize " btn-xs "
         }
        )
  )
(defn- wrap-context
  "Adds the current context to a url"
  [url]
  (str (:context @systemInfo) url)
  )

(def ^:private colour-styles
  "Maps clearances to foreground and background colours"
  {:IR {:color "White" :background-color "Black"}
   :R {:color "White" :background-color "DarkRed"}
   :O {:color "White" :background-color "#DF7C00"}
   :Y {:color "White" :background-color "GoldenRod"}
   :G {:color "White" :background-color "Green"}
   :B {:color "White" :background-color "MediumBlue"}
   :I {:color "White" :background-color "Indigo"}
   :V {:color "White" :background-color "DarkViolet"}
   :U {:color "Black" :background-color "White"}
   }
  )

(defn- get-citizen-basic
  "Gets basic citizen info"
  [citizenId]
  (GET (wrap-context "/api/citizen/basic/")
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
  (GET (wrap-context "/api/citizen/associates/")
       {:params {:citizenId citizenId
                 :userKey (:userKey @userInfo)}
        :response-format (ajax/json-response-format {:keywords? true})
        :handler #(do (log/trace "Requesting associates of citizen" citizenId)
                      (swap! app-state assoc-in [:citizens citizenId :associates] %)
                      )
        }
       )
  )
(defn- add-person-of-interest
  "Adds a poi to app-state. Returns nil on fail"
  [citizenId]
  (log/info "Adding person of interest:" citizenId)
  (let [cid (js/parseInt citizenId)]
    (if (> cid 0)
      (swap! app-state
             update-in [:poi]
             (comp sort
                   distinct
                   #(conj % cid)
                   )
             )
      nil
      )
    )
  )
(defn- remove-person-of-interest
  "Removes a poi in app-state. Returns new app state"
  [citizenId]
  (log/info "Removing person of interest:" citizenId)
  (let [cid (js/parseInt citizenId)]
    (swap! app-state
           update-in [:poi]
           #(remove (set [cid]) %)
           )
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
        [:span {:class "btn btn-default"
               :style (-> {:margin "2px"}
                          (#(if @expand
                              ;(merge % {:border "1px solid white"})
                              %))
                          (merge (get colour-styles (keyword (:clearance cmap)) {:color "Red"}))
                          )
               :onClick #(if (not (:citizenId cmap)) (get-citizen-basic n) nil)
               }
         (if @expand
           ;; Expanded Citizen Info
           [:span
            [:span {:class (str "btn btn-default " (:buttonSize @systemInfo))
                    :onClick #(do (js/console.log (str "Clicked on person:" n))
                                  (swap! expand not)
                                  false)
                    }
             "Name: " (print-person-name cmap)] [:br]
            [:div {:class "btn-group"}
             (if (not (some #{n} (:poi @app-state)))
               [:span {:class (str "btn btn-success " (:buttonSize @systemInfo))
                       :onClick #(add-person-of-interest n)
                       }
                "Add poi"
                ]
               [:span {:class (str "btn btn-warning " (:buttonSize @systemInfo))
                       :onClick #(remove-person-of-interest n)
                       }
                "Remove"
                ]
               )
            ]
            [:div
             "Id Number:" n [:br]
             ]
            (if (:associates cmap)
              ;; Associates known
              [:div {:class "btn-group-vertical"}
               "Known associates:" [:br]
               (for [p (:associates cmap)]
                 [create-citizen-box p])
               ]
              ;; Associates unknown
              [:span {:class (str "btn btn-default " (:buttonSize @systemInfo))
                      :onClick #(get-citizen-associates n)}
               "Get Associates"
               ]
              )

            ]
           ;; Collapsed citizen info
           [:span {:class (str "btn btn-default " (:buttonSize @systemInfo))
                   :onClick #(do (js/console.log (str "Clicked on person:" n))
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
(defn main-page
  []
  (get-citizen-basic (:citizenId @userInfo))
  (let [expand (atom false)
        newPersonVal (atom "")
        ]
    (fn []
      [:div
       [:table {:class "table table-striped table-hover"}
        [:thead
         [:tr
          [:th {:colSpan 3}
           ;; Header Row
           [:span {:class "panel panel-default"} "Welcome Citizen " [create-citizen-box (:citizenId @userInfo)]]
           ;; Select button sizes
           [:span {:class "panel panel-default"}
            [:span "Button Size"]
            [:span {:class (str "btn btn-default" (:buttonSize @systemInfo)) :onClick #(swap! systemInfo assoc-in [:buttonSize] " btn-xs ")} "Extra Small"]
            [:span {:class (str "btn btn-default" (:buttonSize @systemInfo)) :onClick #(swap! systemInfo assoc-in [:buttonSize] " btn-sm ")} "Small"]
            [:span {:class (str "btn btn-default" (:buttonSize @systemInfo)) :onClick #(swap! systemInfo assoc-in [:buttonSize] " ")} "Medium"]
            ]
           ]
          ]
         ]
        [:tr
         ;; Left column. Citizen information
         [:td
          [:h4
           "Persons of Interest:"
           ]
          ;; Adding new persons of interest
          [:div
           [:input {:type "text"
                    :class "form-control"
                    :value @newPersonVal
                    :on-change #(reset! newPersonVal (-> % .-target .-value))
                    :length 8
                    }
            ]
           [:span {:class (str "btn btn-default " (:buttonSize @systemInfo))
                  :onClick #(do
                              (add-person-of-interest @newPersonVal)
                              (reset! newPersonVal "")
                              )
                  }
            "Add person of interest"
            ]
           ]
          [:div {:class "btn-group-vertical"}
           ;; Creates a citizen box for each person of information
           (doall (for
                    [n (-> @app-state :poi)]
                    ^{:key n}
                    [create-citizen-box n]
                    ))]
          ]
         ;; Middle left column, surveillance information
         [:td
          "Surveillance information"
          ]
         ;; Middle right column, objectives
         [:td
          "Objectives go here"
          ]
         ;; Right column, forms
         [:td
          "Forms go here"
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
  (let [uVal (atom nil)
        value (atom "")]
    (fn []
      [:div
       (if-let [k (:userKey @userInfo)]
         ;; Has api key, no citizen id
         (do
           (GET (wrap-context "/api/player/login/")
                {:params {:userKey k}
                 :response-format (ajax/json-response-format {:keywords? true})
                 :handler (fn [m]
                            (swap! userInfo merge m)
                            (log/info "userInfo is now:" @userInfo)
                            )
                 }
                )
           "Logging in, please wait..."
           )
         ;; No api key
         [:div
          "Please enter login key:" [:br]
          [:input {:type "text" :value @value :on-change #(reset! value (-> % .-target .-value))}] [:br]
          [:span {:class "btn btn-default btn-sm"
                  :onClick #(swap! userInfo assoc :userKey @value)}
           "LOG IN"
           ]
          ]
         )
       (if (not @uVal)
         ;; We have not requested a new account
         [:div>span {:class "btn btn-default btn-sm"
                     :onClick #(GET (wrap-context "/api/player/new/")
                                    :response-format (ajax/json-response-format {:keywords? true})
                                    :handler (fn [m]
                                               (reset! uVal (:userKey m))
                                               (log/info "uVal is now:" m))
                                    )
                     }
          "NEW USER"
          ]
         ;; We have requested a new account
         [:div>span "Your new login key: " @uVal [:br]
          "Make sure to save this!"
          ]
         )
       ]
      )
    )
  )
(defn front
  "Render whether it needs the login form or the main page"
  []
  (fn []
    (if (:citizenId @userInfo)
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
