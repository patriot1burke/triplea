@echo off
java -server -Xmx64m -Djava.awt.headless=true -classpath bin\triplea.jar games.strategy.engine.framework.HeadlessGameServer triplea.game.host.console=true triplea.game.host.ui=false triplea.game= triplea.server=true triplea.port=3300 triplea.lobby.host=173.255.229.134 triplea.lobby.port=3303 triplea.name=Bot1_YourServerName triplea.lobby.game.hostedBy=Bot1_YourServerName triplea.lobby.game.supportEmail=yourEmailName(AT)emailProvider.com triplea.lobby.game.comments="automated_hosting"
pause