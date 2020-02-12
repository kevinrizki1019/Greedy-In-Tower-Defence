Java starter bot (modified from starter-bots)

Environment requirements

Install the Java SE Development Kit 8 for your environment here: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Make sure JAVA_HOME system variable is set, Windows 10 tutorial here: https://www.mkyong.com/java/how-to-set-java_home-on-windows-10/

Running
To run the bot place locations of directory /StrAlgo-18073 in the /starter-pack:
starter-pack
  | StrAlgo1-18073
  | examples
  | reference-bot
  | starter-bots
  | visualizer
  | game-config.properties
  | game-runner-config.json
  | run.bat
  | tower-defence-runner-3.0.2.jar
Then go to the edit the config.json file to
`
{
  "round-state-output-location": "./visualizer/tower-defence-matches",
  "game-config-file-location": "./game-config.properties",
  "verbose-mode": true,
  "max-runtime-ms": 2000,
  "player-a": "./StrAlgo1-18073",
  "player-b": "./reference-bot/java",
  "is-tournament-mode": false
}
`
Then run the "run.bat" file on windows or 
`
$mvn install
$mvn run
`
for Linux.

