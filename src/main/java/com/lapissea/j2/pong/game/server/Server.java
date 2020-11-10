package com.lapissea.j2.pong.game.server;

import com.lapissea.util.LogUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static com.lapissea.j2.pong.common.Utils.*;
import static com.lapissea.util.LogUtil.Init.*;

public class Server extends Application{
	
	@Override
	public void start(Stage primaryStage) throws Exception{
		primaryStage.setOnCloseRequest(e->System.exit(0));
		primaryStage.getIcons().add(loadImageFx("icon.png"));
		primaryStage.setTitle("Server view");
		primaryStage.setScene(new Scene(loadFXMLFile(ServerView.class)));
		primaryStage.show();
		
	}
	
	
	public static void main(String[] args){
		LogUtil.Init.attach(USE_CALL_POS|USE_CALL_THREAD|USE_TABULATED_HEADER);
		launch(args);
	}
}
