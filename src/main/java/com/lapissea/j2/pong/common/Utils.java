package com.lapissea.j2.pong.common;

import com.google.gson.GsonBuilder;
import com.lapissea.j2.pong.engine.Status;
import com.lapissea.util.ObjectHolder;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.util.function.UnsafeConsumer;
import com.lapissea.util.function.UnsafeRunnable;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Utils{
	
	public static class Config{
		private final Map<String, Object> data;
		
		public Config(Map<String, Object> data){
			this.data=data;
		}
		
		public Object get(String name){
			Object val=data.get(name);
			Objects.requireNonNull(val, ()->name+" is not present in config");
			return val;
		}
		
		public int getInt(String name){
			var o=get(name);
			if(o instanceof Number n) return n.intValue();
			return Integer.parseInt(o.toString());
		}
		
		public String getString(String name){
			var o=get(name);
			if(o instanceof String s) return s;
			return TextUtil.toString(o);
		}
	}
	
	private static Config CONFIG_CACHE;
	
	private static final String ROOT_START   =".game.";
	private static final String RESOURCE_ROOT="/view/";
	
	private static String getRootName(){
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
		                  .walk(s->s.map(StackWalker.StackFrame::getClassName)
		                            .filter(c->c.contains(ROOT_START))
		                            .map(c->c.substring(c.indexOf(ROOT_START)+ROOT_START.length()))
		                            .map(c->c.substring(0, c.indexOf('.')))
		                            .findFirst()
		                            .orElseThrow());
	}
	
	public static URL makefxmlUrl(Class<?> controllerClass) throws IOException{
		return makefxmlUrl(controllerClass.getSimpleName());
	}
	
	public static GameControlledNode loadGameState(Status status) throws IOException{
		var    loader=new FXMLLoader(makeRawUrl(Path.of(RESOURCE_ROOT, "common", "stages", status.stageName+".fxml").toString()));
		Region parent=loader.load();
		return new GameControlledNode(parent, loader.getController());
	}
	
	public static URL makefxmlUrl(String name) throws IOException{
		return makeUrl(name+".fxml");
	}
	
	public static URL makeRawUrl(String path) throws IOException{
		var resUrl=Utils.class.getResource(path.replace('\\', '/'));
		if(resUrl==null) throw new FileNotFoundException(path.replace('\\', '/')+" could not be found");
		return resUrl;
	}
	
	public static URL makeUrl(String name) throws IOException{
		return makeRawUrl(Path.of(RESOURCE_ROOT, getRootName(), name).toString());
	}
	
	public static Parent loadFXMLFile(Object controller) throws IOException{
		FXMLLoader loader=new FXMLLoader(Utils.makefxmlUrl(controller.getClass()));
		loader.setControllerFactory(c->controller);
		return loader.load();
	}
	
	public static Parent loadFXMLFile(Class<?> controllerClass) throws IOException{
		return loadFXMLFile(controllerClass.getSimpleName());
	}
	
	public static Parent loadFXMLFile(String name) throws IOException{
		return FXMLLoader.load(makefxmlUrl(name));
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized Config getConfig(){
		if(CONFIG_CACHE==null){
			try(var json=makeRawUrl("/config.json").openStream()){
				CONFIG_CACHE=new Config(new GsonBuilder().create().fromJson(new InputStreamReader(json), HashMap.class));
			}catch(IOException e){
				throw new RuntimeException("failed to read config", e);
			}
		}
		return CONFIG_CACHE;
	}
	
	public static void daemonThread(String name, UnsafeRunnable<Exception> action){
		daemonThread(name, action, e->{
			if(e!=null) e.printStackTrace();
		});
	}
	
	public static void daemonThread(String name, UnsafeRunnable<Exception> action, UnsafeConsumer<Throwable, Exception> onEnd){
		Objects.requireNonNull(onEnd);
		Objects.requireNonNull(onEnd);
		UtilL.startDaemonThread(()->{
			Throwable e1=null;
			try{
				action.run();
			}catch(Throwable e){
				e1=e;
			}finally{
				try{
					onEnd.accept(e1);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}, name);
	}
	
	public static void fxThread(Runnable fxAction){
		if(Platform.isFxApplicationThread()){
			fxAction.run();
		}else{
			Platform.runLater(fxAction);
		}
	}
	
	public static void alertFXPopup(String text){
		fxThread(()->new Alert(Alert.AlertType.NONE, text, ButtonType.OK).showAndWait());
	}
	
	private static Optional<ButtonType> optionFXPopup(String text, ButtonType... types){
		ObjectHolder<Optional<ButtonType>> result=new ObjectHolder<>();
		fxThread(()->result.obj=new Alert(Alert.AlertType.NONE, text, types).showAndWait());
		UtilL.sleepWhile(()->result.obj==null);
		return result.obj;
	}
	
	public static boolean yesNoFXPopup(String text){
		return optionFXPopup(text, ButtonType.YES, ButtonType.NO).orElse(ButtonType.NO)==ButtonType.YES;
	}
	
	public static void okFXPopup(String text){
		optionFXPopup(text, ButtonType.OK);
	}
	
	public static Image loadImageFx(String name) throws IOException{
		return SwingFXUtils.toFXImage(Utils.loadImage(name), null);
	}
	
	public static BufferedImage loadImage(String name) throws IOException{
		try(var in=makeUrl(name).openStream()){
			BufferedImage image=ImageIO.read(in);
			if(image==null) throw new IOException(name+" is not a valid image");
			return image;
		}
	}
}
