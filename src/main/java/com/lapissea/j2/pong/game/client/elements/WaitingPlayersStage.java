package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.game.client.ClientGameSide;
import com.lapissea.util.TextUtil;
import javafx.scene.control.Label;

import java.util.Optional;
import java.util.stream.Stream;

public class WaitingPlayersStage extends GameStageController{
	
	
	public Label elStatus;
	
	@Override
	public void updateState(GameState state, ClientGameSide game){
		int needed=(int)(2-Stream.of(state.getPlayerLeft(), state.getPlayerRight()).filter(Optional::isPresent).count());
		elStatus.setText(needed+" "+TextUtil.plural("player", needed)+" left");
	}
	
}
