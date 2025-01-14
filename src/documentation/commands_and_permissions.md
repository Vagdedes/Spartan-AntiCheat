# <a href="https://www.paypal.com/ncp/payment/EVXKXBD6M5XPC">SPARTAN ONE: Get Java & Bedrock Edition in one jar</a>

## Command Arguments
``<>`` Required command argument<br>
``[]`` Optional command argument<br>
You can hover above commands in-game to get additional descriptions about their functionality.


## Command List

``/spartan menu`` opens the main gui menu.<br>
Permissionss: spartan.info, spartan.manage

``/spartan panic`` enables silent mode and disables punishments for all checks.<br>
Permissions: spartan.manage

``/spartan reload`` reloads spartan's configuration.<br>
Permission: spartan.reload

``/spartan notifications [ticks-frequency]`` toggles the notification system.<br>
Permission: spartan.notifications

``/spartan verbose`` toggles all notifications instead of just ones with high hacking probability.<br>
Permission: spartan.notifications

``/spartan info [player]`` opens a GUI menu with a player's violations info.<br>
Permission: spartan.info

``/spartan kick <player> <reason>`` kicks a player and broadcasts a message.<br>
Permission: spartan.kick

``/spartan toggle <check>`` toggles a certain check.<br>
Permission: spartan.manage

``/spartan toggle-prevention <check>`` toggles a certain check's preventions by making it silent.<br>
Permission: spartan.manage

``/spartan toggle-punishment <check>`` toggles a certain check's punishments.<br>
Permission: spartan.manage

``/spartan bypass <player> <check> <seconds>`` allows a player to bypass a check for a certain amount of time.<br>
Permission: spartan.use_bypass

``/spartan warn <player> <reason>`` warns a player with a reason.<br>
Permission: spartan.warn

``/spartan wave <add/remove/clear/run/list> [player] [command]`` allows you to interact with the wave punishment system.<br>
Permission: spartan.wave

``/spartan proxy-command <command>`` sends a command to the proxy/network of servers. (Example: BungeeCord) [May not always work]<br>
Permissions: spartan.admin, spartan.*

```
/spartan <player> if <condition> equals <result> do <command> executes a conditional command.
/spartan <player> if <condition> contains <result> do <command> executes a conditional command.
/spartan <player> if <number> is-less-than <result> do <command> executes a conditional command.
/spartan <player> if <number> is-greater-than <result> do <command> executes a conditional command.
```
Permission: spartan.condition


## Additional Permissions
spartan.bypass allows you to bypass all checks. (USE THE settings.yml op_bypass OPTION WHEN OP)
spartan.bypass.(check) allows you to bypass a certain check.
spartan.admin, spartan.* gives you all permissions except the bypass ones. (BE CAREFUL WHEN OP)
spartan.bedrock considers a player as a Bedrock client instead of Java client. (BE CAREFUL WHEN OP)
spartan.punishment allows you to bypass the configured punishments of all checks.


## Hovering Above Commands
Spartan supports interactive commands. When you hover above a command, Spartan will create a small hovering box that explains in text what the command does and examples of how you can use it.


## Pressing TAB
Spartan supports command auto-complete. To trigger it, type /spartan and press the button TAB on your keyboard. This functionality takes part as a list of recommendations to save you time.


## Permissions that judge Staff Rank / Ranks
```
spartan.admin
spartan.*
spartan.info
spartan.manage
spartan.reload
spartan.notifications
spartan.kick
spartan.use_bypass
spartan.warn
spartan.wave
spartan.condition
```

## Command Proxy-Transfer Plugins (BungeeCord, Velocity, WaterFall)
https://www.spigotmc.org/resources/27580/<br>
https://www.spigotmc.org/resources/52093/

