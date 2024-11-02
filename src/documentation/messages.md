# <a href="https://vagdedes.com/patreon">Get Java & Bedrock Edition in one jar</a>

## messages.yml Example
```
config_reload: '&8[&2{prefix}&8]&e Config successfully reloaded.'
no_permission: '&cYou don''t have permission to interact with this.'
player_not_found_message: '&cPlayer not found.'
```

## HEX Colors
```
Format: §x§1§2§3§4§5§6
Example: §x§0§8§f§f§1§0 is equivalent to #08ff10
Same applies for most plugins that don't use the standard &#123456 or #123456
```

## Syntax / Placeholders

### Server:
```{online} Amount of server players currently online
{staff} Amount of staff players online defined by Spartan permissions
{server:name} Server’s name gathered from Spartan’s configuration
{server:version} Server’s version gathered from the server itself
{motd} Server list description gathered from the your server’s server.properties file
Plugin:
{prefix} Returns the plugin’s official name
{plugin:version} Returns the plugin’s version name, number or both
Messages:
{line} Changes the line in any text
{amount} Amount related to the execution of a functionality
{reason} Reason related to the execution of a functionality
{message} Message related to the execution of a functionality
{time} Amount of time in seconds related to the execution of a functionality
{info} Information related to the execution of a functionality
{type} Text representing the type of a specific functionality
{space} Creates a space character in any text
{punisher} Returns the name of the console/player who executed the punishment
{reported} Returns the name of the player who was reported
```
### Time:
```
{date:time} Time in hours:minutes:seconds form
{date:d-m-y} Time in day-month-year form
{date:m-d-y} Time in month-day-year form
{date:y-m-d} Time in year-month-day form
{creation} Creation date in year-month-day form related to the execution of a functionality
{expiration} Expiration date in year-month-day form related to the execution of a functionality
```
### Player:
```
{player} Player’s original name
{player:type} Player’s type (Example: java, bedrock)
{uuid} Player’s unique identifier text
{ping} Player’s ping in non-decimal form
{world} Player’s current world name
{health} Player’s health in non-decimal form with each number representing half a heart
{gamemode} Player’s gamemode in text form
{x} Player’s X coordinate in non-decimal form
{y} Player’s Y coordinate in non-decimal form
{z} Player’s Z coordinate in non-decimal form
{yaw} Player’s Yaw coordinate in non-decimal form
{pitch} Player’s Pitch coordinate in non-decimal form
{cps} Player’s amount of recent clicks per second
Detection Notifications:
{detections} List of detections with their custom names
{detection} A check’s custom name gathered from Spartan’s configuration
{detection:real} A check’s real name set by Spartan’s developer/s
{silent:detection} True/False depending if a check is silent, thus allowing it to prevent player interactions
{punish:detection} True/False depending if a check can and is configured to punish with commands
```
### Detection:
```
{vls:detection} Player’s amount of violations on a given check.
{vls:percentage} The percentage determining the certainty of a check that a given player is hacking.
```
