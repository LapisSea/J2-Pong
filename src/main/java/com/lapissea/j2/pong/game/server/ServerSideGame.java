package com.lapissea.j2.pong.game.server;

import com.lapissea.j2.pong.common.Utils;
import com.lapissea.j2.pong.common.chat.RMI;
import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.j2.pong.engine.Status;
import com.lapissea.j2.pong.engine.net.ActionSocket;
import com.lapissea.j2.pong.engine.net.NetMessage;
import com.lapissea.j2.pong.game.Message;
import com.lapissea.util.MathUtil;
import com.lapissea.util.NotImplementedException;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import net.coobird.thumbnailator.Thumbnailator;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.lapissea.j2.pong.engine.Status.*;
import static com.lapissea.util.PoolOwnThread.*;

public class ServerSideGame{
	
	private class Connection{
		private final ActionSocket socket;
		
		protected long profileId=newUID();
		protected long ping     =100;
		private   long stamp;
		
		Profile profile;
		
		Connection(Socket socket) throws IOException{
			this.socket=new ActionSocket(socket);
		}
		
		@Override
		public String toString(){
			return profile==null?profileId+"":profile.userName();
		}
		
		void write(NetMessage message) throws IOException{
			socket.write(message);
		}
		
		private void readMessage(NetMessage message) throws IOException{
			if(message.execute(socket)) return;
			
			if(message instanceof NetMessage.Pong msg){
				ping=msg.calcPing();
			}else if(message instanceof NetMessage.MessageSend msg){
				if(msg.text().isBlank()) return;
				
				log(profile.userName(), "says:", '"'+msg.text()+'"');
				
				synchronized(chatHistory){
					chatHistory.add(new Message(profileId, msg.text()));
				}
				writeForEachCon(new NetMessage.MessageBroadcast(new Message(profileId, msg.text())));
			}else if(message instanceof NetMessage.FetchProfileRequest fp){
				long    id=fp.profileId();
				Profile p =profiles.get(id);
				
				if(p==null){
					log(profile.userName(), "is asking for unknown profile id", id);
					write(new NetMessage.FetchProfileFail(id));
				}else{
					if(p.id()!=id) throw new IllegalStateException();
					
					log(profile.userName(), "is asking for profile data of", p.userName());
					write(new NetMessage.FetchProfile(p));
				}
			}else if(message instanceof NetMessage.Login login){
				synchronized(connections){
					if(connections.size()>2){
						write(new NetMessage.LoginResult(false, -1, "Server is full"));
						return;
					}
				}
				
				
				var p=login.getProfile();
				
				var pName=p.userName();
				
				synchronized(connections){
					if(connections.stream().anyMatch(c->c.profile!=null&&c.profile.userName().equals(pName))){
						write(new NetMessage.LoginResult(false, -1, "Username in use"));
						return;
					}
				}
				
				profiles.values()
				        .stream()
				        .filter(p1->p1.userName().equals(pName))
				        .mapToLong(Profile::id)
				        .findAny()
				        .ifPresent(recycleId->profileId=recycleId);
				
				p=p.withId(profileId);
				
				var img=p.icon();
				
				if(img.getWidth()!=img.getHeight()){
					var s =Math.min(img.getWidth(), img.getHeight());
					var bi=new BufferedImage(s, s, img.getType());
					var g =bi.createGraphics();
					g.drawImage(img, (s-img.getWidth())/2, (s-img.getHeight())/2, null);
					g.dispose();
					img=bi;
				}
				
				profile=p.withIcon(Thumbnailator.createThumbnail(img, 50, 50));
				log(profile.userName(), "logged in");
				
				profiles.put(profileId, profile);
				
				saveGameConfig();
				
				write(new NetMessage.LoginResult(true, profileId, null));
				write(new NetMessage.StatusChange(status));
				
				synchronized(chatHistory){
					for(var msg : chatHistory){
						write(new NetMessage.MessageBroadcast(msg));
					}
				}
				
				state.playerJoined(profile.id());
				
			}else if(message instanceof NetMessage.ExitNotice){
				socket.close();
				stamp=Long.MAX_VALUE;
			}else if(message instanceof NetMessage.SignalReady){
				state.playerReady(profileId, true);
			}else if(message instanceof NetMessage.PlayerMovement result){
				state.getPlayer(profileId).setMovement(MathUtil.snap(result.movement(), -1, 1));
			}else throw new NotImplementedException(TextUtil.toString(message));
		}
		
		public void run() throws IOException{
			Utils.daemonThread("CRead-"+profileId, ()->{
				while(!socket.isClosed()){
					readMessage(socket.read());
				}
			}, e->{
//				if(e!=null) e.printStackTrace();
				socket.close();
			});
			
			
			while(!socket.isClosed()){
				UtilL.sleepWhile(()->stamp+2000>System.currentTimeMillis(), 10);
				stamp=System.currentTimeMillis();
				try{
					write(new NetMessage.Ping());
				}catch(Throwable e){
					socket.close();
					return;
				}
			}
		}
	}
	
	private void writeForEachCon(NetMessage message){
		synchronized(connections){
			for(Connection connection : connections){
				try{
					connection.write(message);
				}catch(IOException e){
					log(connection, "failed");
				}
			}
		}
	}
	
	private final GameState        state;
	private final List<Connection> connections=new ArrayList<>();
	
	private final Map<Long, Profile> profiles=new HashMap<>();
	
	private final List<Message> chatHistory=new ArrayList<>();
	
	private long        uid;
	private Set<Status> status=EnumSet.of(RUNNING, WAITING_PLAYERS);
	
	private final Consumer<String>      logFun;
	private final Consumer<Set<Status>> statusChange;
	
	public ServerSideGame(Consumer<String> logFun, Consumer<Set<Status>> statusChange){
		this.logFun=logFun;
		this.statusChange=statusChange;
		
		if(Utils.getConfig().getBool("useRMI")){
			RMI.makeService(667, (message, profileId)->{
				connections.stream().filter(c->c.profileId==profileId).findAny().ifPresent(connection->{
					try{
						connection.readMessage(new NetMessage.MessageSend(message));
					}catch(IOException e){
						e.printStackTrace();
					}
				});
			});
		}
		
		state=new GameState(()->{}, ()->{
			log("game done");
			resetState();
			setStatus(RESULT);
			saveGameConfig();
		}, 20);
		
		loadGameConfig();
		saveGameConfig();
		
		state.listenChange(this::applyGameState);
		
		state.isRunning.addListener(e->{
			if(state.isRunning.get()){
				setStatus(RUNNING);
			}else{
				setStatus(RUNNING, WAITING_START);
			}
		});
		state.canRun.addListener(e->{
			if(state.canRun.get()){
				setStatus(RUNNING, WAITING_START);
			}else{
				setStatus(RUNNING, WAITING_PLAYERS);
			}
		});
		
		state.playerSize.addListener(e->applyGameState());
		state.playerSpeed.addListener(e->applyGameState());
		state.ballSpeed.addListener(e->applyGameState());
	}
	
	private void saveGameConfig(){
		Persistence.saveGameState(state, profiles);
	}
	private void loadGameConfig(){
		Persistence.loadGameState(state, profile->{
			uid=Math.max(uid, profile.id());
			profiles.putIfAbsent(profile.id(), profile);
		});
	}
	
	private void resetState(){
		state.reset();
	}
	
	private synchronized long newUID(){
		return ++uid;
	}
	
	public void start(){
		Utils.daemonThread("SListen", this::run);
	}
	
	private void run() throws IOException{
		statusChange.accept(status);
		
		Utils.daemonThread("GameLoop", ()->{
			while(true){
				UtilL.sleepUntil(state.isRunning::get, 50);
				state.run();
			}
		});
		
		ServerSocket socket=new ServerSocket(Utils.getConfig().getInt("connectPort"));
		while(true){
			
			log("Waiting for player");
			Socket con=socket.accept();
			
			var servCon=new ServerSocket(0);
			
			var out=con.getOutputStream();
			new DataOutputStream(out).writeInt(servCon.getLocalPort());
			out.flush();
			out.close();
			con.close();
			
			var soc=servCon.accept();
			var c  =new Connection(soc);
			
			synchronized(connections){
				connections.add(c);
			}
			
			Utils.daemonThread("Client-"+c.profileId, ()->{
				try{
					log("Connected:", c.profileId, "at", soc.getInetAddress());
					c.run();
				}finally{
					synchronized(connections){
						connections.remove(c);
						log("Disconnected:", c);
						state.playerLeft(c.profile);
					}
					servCon.close();
				}
			}, e->{
				if(e==null) return;
				e.printStackTrace();
			});
			
		}
	}
	
	private       CompletableFuture<Void> syncThrottleTask;
	private       long                    syncStamp;
	private final ExecutorService         exec=Executors.newSingleThreadExecutor();
	
	private void applyGameState(){
		if(syncThrottleTask==null){
			syncThrottleTask=async(()->{
				var elapsed =syncStamp-System.currentTimeMillis();
				var throttle=Math.max(0, Math.max(getMaxPing()*2, 20)+elapsed);
//				LogUtil.printTable("elapsed", elapsed, "throttle", throttle);
				UtilL.sleep(throttle);
				syncThrottleTask=null;
				syncStamp=System.currentTimeMillis();
				
				writeForEachCon(new NetMessage.GameStateBroadcast(state));
				
				saveGameConfig();
			}, exec);
		}
	}
	
	
	public void setStatus(Status... status){
		setStatus(EnumSet.copyOf(List.of(status)));
	}
	
	public void setStatus(Set<Status> status){
		if(status.equals(this.status)) return;
		this.status=status;
		statusChange.accept(status);
		
		writeForEachCon(new NetMessage.StatusChange(status));
		applyGameState();
	}
	
	private void log(Object... message){
		logFun.accept(TextUtil.toString(message));
	}
	
	public GameState getState(){
		return state;
	}
	
	public long getMaxPing(){
		return connections.stream().mapToLong(c->c.ping).max().orElse(100);
	}
}
