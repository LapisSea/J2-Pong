package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Player;
import com.lapissea.j2.pong.game.Message;
import com.lapissea.j2.pong.game.client.ClientGameSide;
import com.lapissea.util.TextUtil;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ResultStage extends GameStageController{
	
	
	public Label    elStatus;
	public Button   elReadyButton;
	public FlowPane elResultDest;
	
	private GameState state;
	
	@Override
	public void updateState(GameState state, ClientGameSide game){
		this.state=state;
		elReadyButton.setDisable(state.getThisPlayer().isReady());
		
		int needed=(int)(2-Stream.of(state.getPlayerLeft(), state.getPlayerRight()).filter(Optional::isPresent).map(Optional::get).filter(Player::isReady).count());
		elStatus.setText(needed+" "+TextUtil.plural("player", needed)+" not ready");
		
		Consumer<Optional<Player>> add=pll->{
			var player=pll.orElse(null);
			if(player!=null){
				try{
					elResultDest.getChildren().add(new MessageBox(game::getProfile, new Message(player.getProfileId(), player.getWins()+" wins")));
				}catch(IOException ignored){ }
			}
		};
		elResultDest.getChildren().clear();
		add.accept(state.getPlayerLeft());
		add.accept(state.getPlayerRight());
	}
	
	public void signalReady(){
		state.signalReady();
	}
}
