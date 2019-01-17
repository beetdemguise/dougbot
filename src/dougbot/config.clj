(ns dougbot.config
  (:require [discord.types :as types]))

(defn get-auth-config []
  (types/->SimpleAuth(System/getenv "BOT_TOKEN")))
