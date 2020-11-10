package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.game.client.ClientGameSide;

public abstract class GameStageController{
	
	public abstract void updateState(GameState state, ClientGameSide game);
}
