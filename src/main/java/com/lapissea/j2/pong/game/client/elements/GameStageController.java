package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import javafx.beans.property.ObjectProperty;

import java.util.function.LongFunction;

public abstract class GameStageController{
	
	public abstract void updateState(GameState state, LongFunction<ObjectProperty<Profile>> profileSource);
}
