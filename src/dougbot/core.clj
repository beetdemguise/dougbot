(ns dougbot.core
  (:require [clojure.string :as str]
            [discord.bot :as discord]
            [dougbot.config :as config]
            [dougbot.commands.terraria]
            [mount.core :as mount]))

(mount/defstate bot
  :start (discord/create-bot "dougbot" @discord/extension-registry "!" (config/get-auth-config))
  :stop (.close bot))

(defn read-user-input []
  (print "Press Q to stop the bot: ")
  (flush)
  (when-not (= (str/lower-case (read-line)) "q")
      read-user-input))

(defn -main [& args]
  (mount/start )
  (trampoline read-user-input)
  (mount/stop))
