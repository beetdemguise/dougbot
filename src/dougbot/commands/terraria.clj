(ns dougbot.commands.terraria
  (:require [amazonica.aws.ec2 :as aws]
            [clojure.core.async :as async]
            [discord.bot :as bot]
            [discord.embeds :as embed]
            [discord.http :as discord]))

(defn get-instance-info []
  (get-in (aws/describe-instances :filters [{:key "tag:name"
                                             :value "Terraria"}])
          [:reservations 0 :instances 0]))

(def terraria-instance-id
  (:instance-id (get-instance-info)))

(defn get-status []
  (as-> terraria-instance-id $
    (aws/describe-instance-status :instance-id $)
    (:instance-statuses $)
    (drop-while #(not= (:instance-id %)) $)
    (first $)
    (get-in $ [:instance-state :code])))

(defn is-pending? []
  (= 0 (get-status)))

(defn is-running? []
  (= 16 (get-status)))

(defn build-status-message []
  (let [instance-emote (format ":%s:" (condp = (get-status)
                                        0 "blue_circle"
                                        16 "white_check_mark"
                                        "red_circle"))
        ip-address (:public-ip-address (get-instance-info))
        text (str instance-emote (when ip-address
                                   (format "IP Address `%s`" ip-address)))]
    (-> (embed/create-embed :title "Terraria Status")
        (embed/+field "EC2 Instance Status" text))))

(defn send-status-message [auth channel]
  (discord/send-message auth channel "" :embed (build-status-message)))

(defn update-status-message [auth channel message]
  (discord/edit-message auth channel message "" :embed (build-status-message)))

(bot/defextension terraria [{:keys [auth]} {:keys [channel]}]
  "Commands to interact with the Terraria EC2 instance"
  (:status
   "Shows the current status of the instance"
   (bot/say (build-status-message)))

  (:start
   "Starts the instance"
   (if (is-running?)
     (bot/say (build-status-message))
     (let [_ (bot/say "Starting up Terraria instance...")
           bot-message (send-status-message auth channel)]
       (aws/start-instances :instance-ids [terraria-instance-id])
       (while (not (is-running?))
         (async/<!! (async/timeout 5000)))
       (update-status-message auth channel bot-message))))

  (:stop
   "Stops the instance"
   (if-not (is-running?)
     (bot/say (build-status-message))
     (let [_ (bot/say "Stopping Terraria instance...")
           bot-message (send-status-message auth channel)]
       (aws/stop-instances :instance-ids [terraria-instance-id])
       (while (some? (get-status))
         (async/<!! (async/timeout 5000)))
       (update-status-message auth channel bot-message)))))
