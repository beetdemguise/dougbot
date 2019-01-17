(ns dougbot.commands.terraria
  (:require [amazonica.aws.ec2 :as aws]
            [discord.bot :as discord]))

(def terraria-instance-id
  (get-in (aws/describe-instances :filters [{:key "tag:name" :value "Terraria"}])
          [:reservations 0 :instances 0 :instance-id]))

(defn is-running? []
  (as-> terraria-instance-id $
    (aws/describe-instance-status :instance-id $)
    (:instance-statuses $)
    (drop-while #(and (not= (:instance-id %))
                      ;; 0 = pending
                      ;; 16 = running
                      (not (contains? [0 16] (get-in % [:instance-state :code]))))
                $)
    (seq $)))

(discord/defextension terraria [client message]
  "Commands to interact with the Terraria EC2 instance"
  (:status
   "Shows the current status of the instance"
   (discord/say (if (is-running?)
                  "Instance is currently up-and-running."
                  "The instance is not started.")))

  (:start
   "Starts the instance"
   (if (is-running?)
     (discord/say "Instance is already running.")
     (do
       (aws/start-instances :instance-ids [terraria-instance-id])
       ;; TODO: start actual tshock server
       (discord/say "Instance is running"))))

  (:stop
   "Stops the instance"
   (if-not (is-running?)
     (discord/say "Instance isn't running.")
     (do
       ;; TODO: stop tshock server before stopping instance
       (aws/stop-instances :instance-ids [terraria-instance-id])
       (discord/say "Instance is stopping.")))))
