package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.util.TextUtil;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Label;

import java.util.Optional;
import java.util.function.LongFunction;
import java.util.stream.Stream;

public class WaitingPlayersStage extends GameStageController{
	
	
	public Label elStatus;
	
	@Override
	public void updateState(GameState state, LongFunction<ObjectProperty<Profile>> profileSource){
		int needed=(int)(2-Stream.of(state.getPlayerLeft(), state.getPlayerRight()).filter(Optional::isPresent).count());
		elStatus.setText(needed+" "+TextUtil.plural("player", needed)+" left");
	}
	
}
