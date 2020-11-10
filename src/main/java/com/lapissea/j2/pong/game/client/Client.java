package com.lapissea.j2.pong.game.client;

import com.lapissea.util.LogUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static com.lapissea.j2.pong.common.Utils.*;
import static com.lapissea.util.LogUtil.Init.*;

public class Client extends Application{
	
	@Override
	public void start(Stage primaryStage) throws Exception{
		primaryStage.getIcons().add(loadImageFx("icon.png"));
		primaryStage.setTitle("Login");
		primaryStage.setScene(new Scene(loadFXMLFile(new LoginView(primaryStage))));
		primaryStage.show();
	}
	
	@Override
	public void stop() throws Exception{
	
	}
	
	public static void main(String[] args){
		LogUtil.Init.attach(USE_CALL_POS|USE_TABULATED_HEADER);
		launch(args);
	}
}
