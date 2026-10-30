PAY MOD  (Fabric, Minecraft 1.21.1)

COMMANDS
  /ui                     toggle the money panel on the right side of the screen
  /set money <amount>     set your own money   (also: /setmoney <amount>)
  /pay <player> <amount>  pay an online player; amount supports k, m, b, t
                          e.g. /pay Vyntherr 1m   /pay Vyntherr 2.5k   /pay Vyntherr 35774
  Paying shows "You paid <name> $<amount>" in chat and plays a ding.

BUILD (needs Java 21 + Gradle 8.8+ , or just open the folder in IntelliJ IDEA)
  gradle build
  -> the mod jar is in build/libs/paymod-1.0.0.jar (not the -sources one)

INSTALL
  Put the jar in your .minecraft/mods folder together with Fabric API 0.102.0+1.21.1
  Install it on both the client and the server (or just singleplayer).

NOTES
  - Balances are saved in config/paymod_balances.json
  - /set money has no permission check, so anyone can give themselves money.
  - To change the ding, edit SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP in PayMod.java
