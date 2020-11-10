package com.lapissea.j2.pong.game.client;

import com.lapissea.j2.pong.common.Utils;
import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.j2.pong.engine.Status;
import com.lapissea.j2.pong.engine.net.ActionSocket;
import com.lapissea.j2.pong.engine.net.NetMessage;
import com.lapissea.j2.pong.game.Message;
import com.lapissea.util.NotImplementedException;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ClientGameSide{
	private GameState state;
	
	private boolean      closed;
	private int          profileId=-1;
	private ActionSocket socket;
	
	private final Consumer<Message>     onMessage;
	private final Consumer<Set<Status>> statusChange;
	private final Consumer<GameState>   updateGameState;
	
	private final Map<Integer, ObjectProperty<Profile>> profiles=new HashMap<>();
	
	private BufferedImage loadingIcon, errorIcon;
	
	private BufferedImage loadIcon(String name){
		try{
			return Utils.loadImage("LoadingProfileIcon.png");
		}catch(IOException e){
			e.printStackTrace();
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
	}
	
	private BufferedImage getErrorIcon(){
		if(errorIcon==null) errorIcon=loadIcon("ErrorProfileIcon.png");
		return loadingIcon;
	}
	
	private BufferedImage getLoadingIcon(){
		if(loadingIcon==null) loadingIcon=loadIcon("LoadingProfileIcon.png");
		return loadingIcon;
	}
	
	public ClientGameSide(Profile profile, Consumer<Message> onMessage, Consumer<Set<Status>> statusChange, Consumer<GameState> updateGameState) throws IOException{
		this.onMessage=onMessage;
		this.statusChange=statusChange;
		this.updateGameState=updateGameState;
		
		
		int port;
		try(Socket portSocket=new Socket(InetAddress.getLocalHost(), Utils.getConfig().getInt("connectPort"))){
			port=new DataInputStream(portSocket.getInputStream()).readInt();
		}
		
		socket=new ActionSocket(new Socket(InetAddress.getLocalHost(), port));
		state=new GameState(()->{
			try{
				socket.write(new NetMessage.SignalReady());
			}catch(IOException e){
				Utils.okFXPopup("Failed to get ready");
			}
		}, ()->{}, 60);
		
		state.listenChange(()->updateGameState.accept(state));
		
		send(new NetMessage.Login(profile));
		
		updateGameState.accept(state);
		
		Utils.daemonThread("Net-in", ()->{
			while(!socket.isClosed()&&!closed){
				readAction(socket.read());
			}
		}, e->{
			if(closed) return;
			if(e!=null) e.printStackTrace();
			Utils.okFXPopup("Connection to the server was lost! Exiting...");
			System.exit(0);
		});
		
		Utils.daemonThread("GameLoop", ()->{
			while(true){
				UtilL.sleepUntil(state.isRunning::get, 50);
				state.run();
			}
		});
	}
	
	private void readAction(NetMessage message) throws IOException{
		if(message.execute(socket)) return;
		if(message instanceof NetMessage.LoginResult result){
			if(result.success()){
				profileId=result.profileId();
				state.initClientId(profileId);
			}else{
				Utils.okFXPopup("Failed to login. Exiting... Reason: "+result.reason());
				System.exit(0);
			}
		}else if(message instanceof NetMessage.MessageBroadcast result){
			onMessage.accept(result.messageSend());
		}else if(message instanceof NetMessage.FetchProfileFail result){
			getProfile(result.profileId()).set(new Profile("<error>", getErrorIcon(), result.profileId()));
		}else if(message instanceof NetMessage.FetchProfile result){
			getProfile(result.getProfile().id()).set(result.getProfile());
		}else if(message instanceof NetMessage.StatusChange result){
			statusChange.accept(result.statuses());
		}else if(message instanceof NetMessage.GameStateBroadcast result){
			state.read(result.open());
		}else throw new NotImplementedException(TextUtil.toString(message));
	}
	
	public GameState getState(){
		return state;
	}
	
	void send(NetMessage message) throws IOException{
		if(profileId==-1&&(!(message instanceof NetMessage.Login)&&!(message instanceof NetMessage.Pong))){
			throw new IllegalStateException("not logged in, can't send "+TextUtil.toString(message));
		}
		socket.write(message);
	}
	
	public void sendMessage(String message) throws IOException{
		send(new NetMessage.MessageSend(message));
	}
	
	public synchronized ObjectProperty<Profile> getProfile(int id){
		return profiles.computeIfAbsent(id, this::requestProfile);
	}
	
	private ObjectProperty<Profile> requestProfile(int id){
		try{
			send(new NetMessage.FetchProfileRequest(id));
		}catch(IOException e){
			getProfile(id).set(new Profile("<error>", getErrorIcon(), id));
		}
		return new SimpleObjectProperty<>(new Profile("<loading>", getLoadingIcon(), id));
	}
	
	public void close(){
		try{
			closed=true;
			send(new NetMessage.ExitNotice());
			socket.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void notifyPlayerMovement(float movement){
		var tp=state.getThisPlayer();
		if(Math.abs(tp.getMovement()-movement)<0.00001) return;
		tp.setMovement(movement);
		try{
			send(new NetMessage.PlayerMovement(movement));
		}catch(IOException e){
			Utils.okFXPopup("Connection to the server was lost! Exiting...");
			System.exit(0);
		}
	}
}
