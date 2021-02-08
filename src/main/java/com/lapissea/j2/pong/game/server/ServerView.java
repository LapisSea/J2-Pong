package com.lapissea.j2.pong.game.server;

import com.lapissea.j2.pong.common.GameControlledNode;
import com.lapissea.j2.pong.engine.Status;
import com.lapissea.util.TextUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

import static com.lapissea.j2.pong.common.Utils.*;

public class ServerView{
	
	public Label      elStatus;
	public VBox       elLog;
	public ScrollPane elLogScroll;
	public Slider     elPlayerSize;
	
	private final ServerSideGame game=new ServerSideGame(
		msg->fxThread(()->elLog.getChildren().add(new Label(msg))),
		state->fxThread(()->elStatus.setText(state.toString()))
	);
	
	public Slider     elPlayerSpeed;
	public Slider     elBallSpeed;
	public AnchorPane elGameView;
	public Label      elInfo;
	
	
	@FXML
	private void initialize() throws IOException{
		elLogScroll.vvalueProperty().bind(elLog.heightProperty());
		game.start();
		
		elPlayerSize.setValue(game.getState().playerSize.get());
		elPlayerSpeed.setValue(game.getState().playerSpeed.get());
		elBallSpeed.setValue(game.getState().ballSpeed.get());
		
		elPlayerSize.valueProperty().addListener(e->game.getState().playerSize.set((float)elPlayerSize.getValue()));
		elPlayerSpeed.valueProperty().addListener(e->game.getState().playerSpeed.set((float)elPlayerSpeed.getValue()));
		elBallSpeed.valueProperty().addListener(e->game.getState().ballSpeed.set((float)elBallSpeed.getValue()));
		
		
		GameControlledNode stageNode=loadGameState(Status.RUNNING);
		
		Region parent=stageNode.node();
		
		elGameView.getChildren().add(parent);
		var state=game.getState();
		
		AnchorPane.setTopAnchor(parent, 0D);
		AnchorPane.setBottomAnchor(parent, 0D);
		AnchorPane.setLeftAnchor(parent, 0D);
		AnchorPane.setRightAnchor(parent, 0D);
		
		Runnable doChange=()->{
			elInfo.setText(TextUtil.toNamedPrettyJson(state.playerStream().toArray()));
			
			stageNode.controller().updateState(state, value->new SimpleObjectProperty<>());
		};
		
		state.listenChange(()->fxThread(doChange));
		
		var n=elGameView;
		n.heightProperty().addListener(e->doChange.run());
		n.widthProperty().addListener(e->{
			doChange.run();
			n.setPrefHeight(n.getWidth());
		});
		
		Platform.runLater(doChange);
		doChange.run();
	}
	
}
