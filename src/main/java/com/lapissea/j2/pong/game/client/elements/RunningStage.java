package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.function.LongFunction;

public class RunningStage extends GameStageController{
	
	public AnchorPane elLeftPlArea;
	public Pane       elLeftPl;
	
	public AnchorPane elRightPlArea;
	public Pane       elRightPl;
	
	public AnchorPane elBallArea;
	public Pane       elBall;
	
	@Override
	public void updateState(GameState state, LongFunction<ObjectProperty<Profile>> profileSource){
		var ball=state.getBall();
		var pos =ball.pos;
		
		updateHeight(state, elLeftPl, elLeftPlArea);
		updateHeight(state, elRightPl, elRightPlArea);
		
		AnchorPane.setLeftAnchor(elBall, pos.x()*(elBallArea.getWidth()-elBall.getPrefWidth()));
		AnchorPane.setTopAnchor(elBall, pos.y()*(elBallArea.getHeight()-elBall.getPrefHeight()));
		
		var h1=elLeftPlArea.getHeight()-elLeftPl.getPrefHeight();
		AnchorPane.setTopAnchor(elLeftPl, Math.min(state.getPlayerLeftPos()*h1, h1-1));
		var h2=elRightPlArea.getHeight()-elRightPl.getPrefHeight();
		AnchorPane.setTopAnchor(elRightPl, Math.min(state.getPlayerRightPos()*h2, h2-1));
		
	}
	
	private void updateHeight(GameState state, Region el, Region elArea){
		el.setPrefHeight(elArea.getHeight()*state.playerSize.get());
	}
	
	@FXML
	public void initialize(){ }
}
