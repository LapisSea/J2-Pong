package com.lapissea.j2.pong.game.client;

import com.lapissea.j2.pong.common.GameControlledNode;
import com.lapissea.j2.pong.common.Utils;
import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.j2.pong.engine.Status;
import com.lapissea.j2.pong.game.Message;
import com.lapissea.j2.pong.game.client.elements.MessageBox;
import com.lapissea.util.LateInit;
import com.lapissea.util.UtilL;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static com.lapissea.j2.pong.common.Utils.*;
import static java.util.stream.Collectors.*;

public class GameView{
	
	public VBox       elMessages;
	public ScrollPane elScrollMessages;
	
	public TextField elMessageText;
	public Button    elSendButton;
	
	public AnchorPane elStageContainer;
	
	private final LateInit<ClientGameSide> game;
	private       long                     lastProfile=-1;
	
	private final Map<Status, GameControlledNode> stageNodes=new HashMap<>();
	
	private GameState lastState;
	
	public GameView(Stage fxStage, Profile profile){
		fxStage.setOnCloseRequest(e->getGame().close());
		
		game=new LateInit<>(()->{
			while(true){
				try{
					return new ClientGameSide(
						profile,
						msg->fxThread(()->addMessage(msg)),
						newStages->fxThread(()->showStages(newStages)),
						state->fxThread(()->updateState(state))
					);
					
				}catch(IOException e){
					e.printStackTrace();
					if(Utils.yesNoFXPopup("Failed to connect to server! Would you like to retry?")) continue;
					throw UtilL.sysExit(-2);
				}
			}
		});
	}
	
	private void updateState(GameState state){
		lastState=state;
		for(GameControlledNode node : stageNodes.values()){
			if(!node.node().isVisible()) continue;
			node.controller().updateState(state, getGame()::getProfile);
		}
	}
	
	private void showStages(Set<Status> stages){
		for(Status stage : stages){
			if(!stageNodes.containsKey(stage)){
				try{
					
					GameControlledNode stageNode=loadGameState(stage);
					stageNodes.put(stage, stageNode);
					
					Parent parent=stageNode.node();
					parent.setVisible(false);
					
					elStageContainer.getChildren().add(parent);
					
					AnchorPane.setTopAnchor(parent, 0D);
					AnchorPane.setBottomAnchor(parent, 0D);
					AnchorPane.setLeftAnchor(parent, 0D);
					AnchorPane.setRightAnchor(parent, 0D);
				}catch(IOException e){
					e.printStackTrace();
					Utils.okFXPopup("Failed to load "+stage+" stage! Exiting");
					throw UtilL.sysExit(-2);
				}
			}
		}
		
		stageNodes.forEach((k, v)->v.node().setVisible(stages.contains(k)));
		for(Status value : Status.values()){
			if(!stageNodes.containsKey(value)) continue;
			stageNodes.get(value).node().toBack();
		}
		
		if(stages.equals(EnumSet.of(Status.RUNNING))){
			stageNodes.get(Status.RUNNING).node().requestFocus();
		}
		
		Platform.runLater(this::reUpdateState);
	}
	
	private void reUpdateState(){
		if(lastState!=null) updateState(lastState);
	}
	
	private void addMessage(Message msg){
		try{
			if(msg.profileId()==lastProfile){
				elMessages.getChildren().add(new MessageBox(getGame()::getProfile, new Message(-1, msg.text())));
			}else{
				elMessages.getChildren().add(new MessageBox(getGame()::getProfile, msg));
			}
			lastProfile=msg.profileId();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@FXML
	private void initialize(){
		elMessageText.setOnKeyPressed(e->{ if(e.getCode()==KeyCode.ENTER) send(); });
		elMessageText.textProperty().addListener(e->elSendButton.setDisable(elMessageText.getText().isBlank()));
		elScrollMessages.vvalueProperty().bind(elMessages.heightProperty());
		
		Runnable reflow=()->Platform.runLater(this::reUpdateState);
		elStageContainer.widthProperty().addListener(e->reflow.run());
		elStageContainer.heightProperty().addListener(e->reflow.run());
		
		elStageContainer.setOnMouseClicked(e->elStageContainer.requestFocus());
		
	}
	
	public ClientGameSide getGame(){
		return game.get();
	}
	
	public void send(){
		game.ifInited(game->{
			String text=elMessageText.getText().trim();
			elMessageText.setText("");
			
			try{
				game.sendMessage(text);
			}catch(IOException e){
				e.printStackTrace();
			}
		});
	}
	
	private static final List<KeyCode> KEYS_DOWN, KEYS_UP;
	
	static{
		try{
			Function<String, List<KeyCode>> parseKeys=name->{
				return ((List<String>)Utils.getConfig().get(name)).stream()
				                                                  .map(String::toUpperCase)
				                                                  .map(KeyCode::valueOf)
				                                                  .collect(toUnmodifiableList());
			};
			KEYS_DOWN=parseKeys.apply("keysDown");
			KEYS_UP=parseKeys.apply("keysUp");
		}catch(Exception e){
			e.printStackTrace();
			throw UtilL.sysExit(-1);
		}
	}
	
	private boolean downPressed;
	private boolean upPressed;
	
	public void keyDown(KeyEvent keyEvent){
		if(KEYS_DOWN.contains(keyEvent.getCode())) downPressed=true;
		if(KEYS_UP.contains(keyEvent.getCode())) upPressed=true;
		updateMovement();
	}
	
	public void keyUp(KeyEvent keyEvent){
		if(KEYS_DOWN.contains(keyEvent.getCode())) downPressed=false;
		if(KEYS_UP.contains(keyEvent.getCode())) upPressed=false;
		updateMovement();
	}
	
	private void updateMovement(){
		game.ifInited(e->{
			if(e.getState().isRunning.get()){
				e.notifyPlayerMovement(((downPressed?1:0)+(upPressed?-1:0)));
			}
		});
	}
}
