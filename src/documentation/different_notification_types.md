## Using the Right Notifications

### Suspicion Notifications:
Notifications which are not frequent and compressed to contain only what truly matters. These notifications will periodically send a message letting you know some players are hacking and will attach their names so you can easily find them.

### Detection Notifications:
Notifications which are frequent and contain a lot of useful information. These notifications will send a message when a player is violated by a check and contain information about the check violations, the player latency, the server TPS, the detection information, e.t.c. Their frequency will change depending on how close you are to the player, allowing you to focus on what matters.

### Explanation:
When notification frequency is less than 100 ticks, which is worth 5 seconds, Spartan will pick Detection Notifications. If it is 100 ticks or greater, it will pick Detection Notifications along with Suspicion Notifications. To change the frequency, run the command /spartan notifications X, with X being the ticks-frequency of the Detection Notifications. For example, if you pick 10 you will be notified every 0.5 seconds because the server runs 20 times per second, thus “10 / 20 = 0.5”.
