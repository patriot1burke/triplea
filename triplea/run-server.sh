cd $(dirname $0)
java -server  -Xmx64m -classpath bin/triplea.jar:lib/derby_10_1_2.jar -Dtriplea.lobby.port=3303 -Dtriplea.lobby.console=true  games.strategy.engine.lobby.server.LobbyServer 
