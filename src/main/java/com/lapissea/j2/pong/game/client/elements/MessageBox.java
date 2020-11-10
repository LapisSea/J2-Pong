package com.lapissea.j2.pong.game.client.elements;

import com.lapissea.j2.pong.common.Utils;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.j2.pong.game.Message;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.function.IntFunction;

import static com.lapissea.j2.pong.common.Utils.*;

public class MessageBox extends GridPane{
	
	public ImageView elIcon;
	public Label     elUsername;
	public Label     elMessageText;
	
	public MessageBox(IntFunction<ObjectProperty<Profile>> profileSource, Message message) throws IOException{
		
		FXMLLoader loader=new FXMLLoader(Utils.makefxmlUrl(getClass()));
		loader.setRoot(this);
		loader.setController(this);
		loader.load();
		
		if(message.profileId()==-1){
			setVgap(0);
			setPadding(new Insets(2, getPadding().getRight(), 2, getPadding().getLeft()));
			getChildren().remove(elIcon);
			getChildren().remove(elUsername);
		}else{
			ObjectProperty<Profile> prop=profileSource.apply(message.profileId());
			prop.addListener(e->setProfile(prop.get()));
			setProfile(prop.get());
		}
		elMessageText.setText(message.text());
	}
	
	private void setProfile(Profile profile){
		fxThread(()->{
			elIcon.setImage(SwingFXUtils.toFXImage(profile.icon(), null));
			elUsername.setText(profile.userName());
		});
	}
	
}
