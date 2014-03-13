; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-

(ns cljengine.tasks
  (:use (clojure core repl pprint reflect)
        (cljengine mc )
        (cljminecraft core
                      entity
                      [bukkit :exclude [repeated-task
                                        cancel-task]]
                      events
                      commands
                      logging
                      util
                      [world :exclude [effect]]; (effect) has a simple bug.
                      ;; can't pull in all of cljminecraft.player without conflict:
                      [player :only [send-msg]]))
  (:import (org.reflections Reflections)
           (org.bukkit Bukkit
                       Material
                       Location
                       World
                       Effect)
           (org.bukkit.block Block
                             BlockFace ; Enum
                             BlockState)
           (org.bukkit.entity Entity
                              EntityType
                              Player)
           (org.bukkit.metadata Metadatable)
           (org.bukkit.event Event
                             Cancellable
                             EventPriority; Enums
                             HandlerList)
           (org.bukkit.event.entity PlayerDeathEvent)
           (org.bukkit.event.player PlayerMoveEvent)
           (org.bukkit.event.block BlockEvent
                                   BlockPhysicsEvent
                                   BlockBreakEvent)
           (org.bukkit.event.vehicle VehicleBlockCollisionEvent
                                     VehicleMoveEvent)
           (org.bukkit.util Vector
                            BlockVector)
           (org.bukkit.plugin Plugin)
           (org.bukkit.plugin.java JavaPlugin); subtype of Plugin
           (org.bukkit.scheduler BukkitScheduler
                                 BukkitTask)
           (cljminecraft BasePlugin
                         ClojurePlugin)
           (org.bukkit.util BlockIterator)))
(defn get-pending-tasks []
      "Wrapper; checks (scheduler)."
      (.getPendingTasks (scheduler)))





#_(defn repeated-task*
  "Modified version.
Execute a given function repeatedly on the UI thread, delay and period in server ticks. If you specify async?, take care not to directly call any Bukkit API and, by extension, and clj-minecraft functions that use the Bukkit API within this function"
  [plugin callback delay period & {:keys [times async]}]
  (let [schedule-fn (if async (memfn runTaskTimerAsynchronously)
                        (memfn runTaskTimer))]
    (cond*
     [(nil? times)
;      (schedule-fn (scheduler) plugin fn (long delay) (long period))
      ]
     [(zero? times) (callback)]
     [:else
      (delayed-task plugin
                     (fn []
                       (callback)
                       (repeated-task plugin callback ))
                     period)]
     )))


#_(defn schedule-task [callback & {:keys [plugin delay period times async scheduler]
                                 :or [plugin @clj-plugin
                                      delay 0
                                      async false
                                      scheduler (scheduler)]}]
"If 'delay' is logical false, the callback is run immediately (no scheduling).  If 'delay' is 0, it is run on the next tick."
  (cond*
   [(not period)
     (cond*
      [(not delay) (callback)]
      [(zero? delay) (.runTask scheduler plugin callback)]
      [(> delay 0) (.runTaskLater scheduler plugin callback delay)]
      [:else false])]
   [(not times)
    (assert (> period 0))
    (repeated-task plugin callback delay period)]
   [(> times 0)
    (assert (> period 0))
    (if delay (schedule-task plugin scheduler
                              callback
                              :delay nil)
         )]
   [:else nil]))


;; TODO: The functional style I used here works well in Clojure, but is less than long-term practical.
#_(defn ^{:doc "Pretty nice little entry point for the scheduler system.  If you use :delay only, you get something like (delayed-task);
you can try :period and :delay to get (repeated-task); but if you use :times, you get something else entirely: a fixed number of repetitions using BukkitScheduler.runTask()."}
  schedule-repeated-task [callback & {:keys [delay period times plugin
                                        ;                        scheduler
                                                     ]
                                              :or [delay nil
                                        ;                                       plugin (deref clj-plugin)
                                                   period nil
                                                   times nil
                                        ;                      scheduler (scheduler)
                                                   ]}]
  (let [plugin @clj-plugin]
    (assert* (instance? Plugin plugin))
   (if-not delay
     (econd*
      [(not times) (repeated-task plugin callback delay period)]
      [(zero? times) nil]
      [(== 1 times)
       (callback)
;       (debug-println "Done.")
       ]
      [(> times 1)
;       (debug-println times "remaining.")
       (assert* (integer? period) "You must specify a nonnegative number of ticks for repetition.")
       (assert* (>= period 0))
       (callback)
;       (debug-println (dec times) "remaining.")
       (schedule-repeated-task callback
                               :plugin plugin
                               :delay period ; *** Here's the important change
                               :period period
                                        ;   :scheduler scheduler
                               :times (dec times))])
     (do
       (assert* (integer? delay))
                                        ;(debug-println delay)
       (econd*
        [(zero? delay)
         (.runTask (scheduler) plugin callback)]
        [(> delay 0)
;         (debug-println "Rescheduling." delay period times)
         (.runTaskLater (scheduler) plugin
                        #(schedule-repeated-task callback
;                                                 :plugin plugin
                                                 :delay nil
                                                 :period period
                                                 :times times)
                        delay)])))))


(def get-task-id "Wrapper for getTaskId()."
  (memfn getTaskId))




;; TODO: Rewrite with (etypecase*).
(defn task-active? [task]
  "Wrapper; true if 'task' (which may be a BukkitTask or its ID number, either one) is running or waiting to run."
  (econd*
   [(instance? Long task) (or (.isCurrentlyRunning (scheduler) task)
                              (.isQueued (scheduler) task))]
   [(number? task) (task-active? (long task))]
   [(instance? BukkitTask task) (task-active? (.getTaskId task))]))

(defn cancel-task [task]
  "Wrapper; like the cljminecraft func of the same name, but overloaded on param type."
  (cond*
   [(instance? Long task)
    (debug-println "Cancelling task" task)
    (.cancelTask (scheduler) task)]
   ;; The Long requirement was a nasty bug source.
   [(number? task) (cancel-task (long task))]
   ;; The absence of the 'long' cast was a nasty bug:
   [(instance? BukkitTask task) (cancel-task (long (.getTaskId task)))]
   [:else (assert* false (format "Failure in (cancel-task); value %s is of type neither Long nor BukkitTask." task))]))

(defn cancel-all-tasks []
  (doseq [task (get-pending-tasks)]
     (cancel-task task))
  (assert* (zero? (count (get-pending-tasks))))
  (debug-println "** All tasks cancelled.")
  nil)

;;; Borrowed from clj-minecraft with a correction--the original (repeated-task) forgets to include a plugin arg.
#_(defn repeated-task
  "Execute a given function repeatedly on the UI thread, delay and period in server ticks. If you specify async?, take care not to directly call any Bukkit API and, by extension, and clj-minecraft functions that use the Bukkit API within this function"
  [plugin fn delay period & [async?]]
  (if async?
    (.runTaskTimerAsynchronously (scheduler) plugin fn (long delay) (long period))
    (.runTaskTimer (scheduler) plugin fn (long delay) (long period))))

(defn ^BukkitTask repeated-task [func delay period & {:keys [exception-cancel
                                                             units]
                                                      :or {exception-cancel true
                                                           units :ticks}}]
  "A wrapper like clj-minecraft's.  However, I've left out the 'async' and 'plugin' options and added 'exception-cancel', which by default is true: unless you disable it, an uncaught exception within the 'func' callback will result in the task's early removal from the scheduler.
The 'units' defaults to :ticks.  You can also use :seconds."
  (assert* *plugin*)
  (let [[delay period] (econd*
                        [(= units :ticks) [delay period]]
                        [(= units :seconds) (map seconds-to-ticks [delay period])])
        ;; in case I change this later.  Definition borrowed from cljminecraft.bukkit/repeated-task:
        impl-func (fn [^Plugin plugin fn delay period]
                    (.runTaskTimer (scheduler) plugin fn (long delay) (long period)))]
    (assert* (number? delay))
    (assert* (number? period))
    (if-not exception-cancel
      ;; If we don't need a (try) block, just invoke the callback:
      (impl-func *plugin* func delay period)
      ;; Otherwise, make a slot to reference the task, once it's created:
      (let [task-promised (promise)
            ;; ... and wrap the callback in a try-block thunk:
            func' (fn []
                    (assert* (realized? task-promised))
                    (try
                      ;; Orig. callback
                      (func)
                      (catch Error e
                        (when (task-active? task-promised)
                          (debug-println "Error observed in periodic task; auto-cancelling.")
                          (cancel-task task-promised)
                          (assert* (not (task-active? task-promised)))
                          (throw e)))))
            ;; Finally, schedule task:
            task (impl-func *plugin* func' delay period)]
        ;; ... and give it a ref to itself:
        (deliver task-promised task)
        ;; retval:
        task))))
