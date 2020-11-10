package com.lapissea.j2.pong.game.client;

import com.lapissea.j2.pong.common.Utils;
import com.lapissea.j2.pong.engine.Profile;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


public class LoginView{
	public ImageView dropImg;
	public TextField elUsername;
	public Button    elLogin;
	
	private BufferedImage image;
	
	private final Stage stage;
	
	public LoginView(Stage stage){
		this.stage=stage;
	}
	
	
	@FXML
	private void initialize(){
		elUsername.textProperty().addListener(e->{ validate(); });
		dropImg.imageProperty().addListener(e->{ validate(); });
		
		validate();

//		Platform.runLater(()->{
//			try{
//				elUsername.setText("deb #"+Rand.i(10000));
//				var pngs=new File("D:\\Pictures\\seal").listFiles((dir, name)->name.endsWith("png"));
//				image=Objects.requireNonNull(ImageIO.read(pngs[Rand.i(pngs.length)]));
//				login();
//			}catch(IOException e){
//				e.printStackTrace();
//			}
//		});
	}
	
	private void validate(){
		elLogin.setDisable(image==null||elUsername.getText().isBlank());
	}
	
	@SuppressWarnings("unchecked")
	public void imgDragDrop(DragEvent dragEvent){
		try{
			File file=((List<File>)dragEvent.getDragboard().getContent(DataFormat.FILES)).get(0);
			image=Objects.requireNonNull(ImageIO.read(file));
			dropImg.setImage(SwingFXUtils.toFXImage(image, null));
		}catch(Exception ignored){
			Utils.alertFXPopup("Failed to read as an image");
		}
	}
	
	public void imgDragOver(DragEvent dragEvent){
		dragEvent.acceptTransferModes(TransferMode.ANY);
	}
	
	public void login() throws IOException{
		validate();
		if(elLogin.isDisabled()) return;
		
		stage.setTitle("Pong");
		stage.setScene(new Scene(Utils.loadFXMLFile(new GameView(stage, new Profile(elUsername.getText().trim(), image, -1)))));
	}
	
	public void pick(){
		
		FileChooser fileChooser=new FileChooser();
		fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Image files", "png", "jpg", "bmp"));
		File file=fileChooser.showOpenDialog(stage);
		if(file!=null){
			try{
				image=Objects.requireNonNull(ImageIO.read(file));
				dropImg.setImage(SwingFXUtils.toFXImage(image, null));
			}catch(Exception ignored){
				Utils.alertFXPopup("Failed to open file "+file.getName()+" as an image");
			}
		}
	}
}
