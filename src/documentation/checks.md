# <a href="https://www.paypal.com/ncp/payment/EVXKXBD6M5XPC">Get Java & Bedrock Edition in one jar</a>

## checks.yml Example
```
Speed: Check Category
  enabled:
    java: true # True if you want the check to detect Java players for using hack modules
    bedrock: true # True if you want the check to detect Bedrock players for using hack modules
  silent:
    java: true # True if you don't want to cancel, teleport, prevent, e.t.c on violations
    bedrock: true # True if you don't want to cancel, teleport, prevent, e.t.c on violations
  punishments: # Numbers/commands do NOT represent upcoming punishments but a single punishment
    enabled:
      java: true # True if you want the check to be considered in the punishments algorithm
      bedrock: true # True if you want the check to be considered in the punishments algorithm
    commands: # Commands execute all together and in order anytime a player is found to be cheating
      '1': spartan warn {player} You have been detected for using hack modules # First command executed
      '2': spartan kick {player} You were kicked for using: {detections} # Second command executed
      '3': '' # Third command not set
      '4': '' # Fourth command not set
      '5': '' # Fifth command not set
      '6': '' # Sixth command not set
      '7': '' # Seventh command not set
      '8': '' # Eighth command not set
      '9': '' # Ninth command not set
      '10': '' # Tenth command not set
  cancelled_event: false # Whether the check should run when its server event is canceled by the server or another plugin
  name: Speed # Name the check will appear with in most scenarios
  disabled_worlds: exampleDisabledWorld1, exampleDisabledWorld2 # List of worlds the check will not work
  silent_worlds: exampleSilentWorld1, exampleSilentWorld2 # List of worlds the check will not cancel, teleport, prevent, e.t.c on violations
```
(There are more options available, the above is an example of the configuration)
## Canceled Server Events
When a Minecraft server’s events are canceled, it is unlikely our Minecraft plugins will measure this event until it’s allowed again. For example, when the PlayerMoveEvent is canceled on Bukkit-based servers, a player cannot move on the server, leading to the Spartan anti-cheat to ignore checking the player for hack modules.
