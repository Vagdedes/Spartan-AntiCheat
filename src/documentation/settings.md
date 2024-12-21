# <a href="https://www.paypal.com/ncp/payment/EVXKXBD6M5XPC">Get Java & Bedrock Edition in one jar</a>

## settings.yml File
```
Punishments: 
  broadcast_on_punishment: true # Let everyone know when someone is punished via the chat
Logs:
  log_file: true # Log important information in Spartan’s log directory/folder in individual files
  log_console: true # Log important information in the console and logs of your server
Notifications:
  awareness_notifications: true # Be notified about important functionalities of the plugin
  individual_only_notifications: false # Be notified only about your own detections and not of other players
  enable_notifications_on_login: true # Detection notifications will enable automatically upon logging in for players with access
  message_clickable_command: '/teleport {player}' # Command executed when a chat notification message is clicked
Important:
  max_supported_player_latency: 5000 # Allow the plugin to check the player’s performance and prevent instabilities
  op_bypass: false # Bypass the detections if you are a server operator
  bedrock_client_permission: false # Enables the permission “spartan.bedrock” to consider Java clients as Bedrock clients.
  enable_developer_api: true # Toggles on/off the plugin’s code parts that can be controlled by other plugins
  bedrock_player_prefix: '.' # The character needed in the start of a player’s name to identify them as a bedrock player
  enable_npc: true # Allow the plugin to spawn a NPC player when you run the command /spartan
  enable_watermark: true # Allow the plugin to send messages to players that this server is protected by Spartan
  server_name: specify server name # Put the same unique name for each server you have put in your proxy configuration
Detections:
  fall_damage_on_teleport: false # Create artificial fall damage when a player is teleported by a detection
  ground_teleport_on_detection: true # Teleport a player to the ground when identified by a detection
Discord:
  webhook_hex_color: 4caf50
  checks_webhook_url: ''
  punishments_webhook_url: ''
```
