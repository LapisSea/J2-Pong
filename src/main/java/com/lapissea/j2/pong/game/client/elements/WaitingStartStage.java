package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.util.TextUtil;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Optional;
import java.util.function.LongFunction;
import java.util.stream.Stream;

public class WaitingStartStage extends GameStageController{
	
	public Button elReadyButton;
	public Label  elStatus;
	
	private GameState state;
	
	@Override
	public void updateState(GameState state, LongFunction<ObjectProperty<Profile>> profileSource){
		this.state=state;
		elReadyButton.setDisable(state.getThisPlayer().isReady());
		
		int needed=(int)(2-Stream.of(state.getPlayerLeft(), state.getPlayerRight()).filter(Optional::isPresent).map(Optional::get).filter(e->e.isReady()).count());
		elStatus.setText(needed+" "+TextUtil.plural("player", needed)+" not ready");
	}
	
	public void signalReady(){
		state.signalReady();
	}
}
