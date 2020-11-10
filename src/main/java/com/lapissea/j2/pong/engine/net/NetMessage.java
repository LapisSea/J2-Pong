package com.lapissea.j2.pong.engine.net;

import com.lapissea.j2.pong.engine.GameState;
import com.lapissea.j2.pong.engine.Profile;
import com.lapissea.j2.pong.engine.Status;
import com.lapissea.j2.pong.game.Message;

import java.io.*;
import java.util.Set;

public interface NetMessage{
	
	record Ping(long stamp) implements Serializable, NetMessage{
		public Ping(){
			this(System.currentTimeMillis());
		}
		
		@Override
		public boolean execute(ActionSocket socket) throws IOException{
			socket.write(new Pong(stamp));
			return true;
		}
	}
	
	record Pong(long stamp) implements Serializable, NetMessage{
		public long calcPing(){
			return System.currentTimeMillis()-stamp;
		}
	}
	
	record Login(Profile.Ball profile) implements Serializable, NetMessage{
		
		public Login(Profile profile){ this(new Profile.Ball(profile)); }
		
		public Profile getProfile()  { return profile.get(); }
	}
	
	record LoginResult(boolean success, int profileId, String reason) implements Serializable, NetMessage{}
	
	record MessageBroadcast(Message messageSend) implements Serializable, NetMessage{}
	
	record MessageSend(String text) implements Serializable, NetMessage{}
	
	record FetchProfileFail(int profileId) implements Serializable, NetMessage{}
	
	record FetchProfileRequest(int profileId) implements Serializable, NetMessage{}
	
	record FetchProfile(Profile.Ball profile) implements Serializable, NetMessage{
		public FetchProfile(Profile profile){ this(new Profile.Ball(profile)); }
		
		public Profile getProfile()         { return profile.get(); }
	}
	
	record StatusChange(Set<Status> statuses) implements Serializable, NetMessage{}
	
	record ExitNotice() implements Serializable, NetMessage{}
	
	record GameStateBroadcast(byte[] data) implements Serializable, NetMessage{
		private static byte[] toBytes(GameState state){
			ByteArrayOutputStream buffer=new ByteArrayOutputStream();
			try{
				state.write(new DataOutputStream(buffer));
			}catch(IOException e){
				throw new RuntimeException(e);
			}
			return buffer.toByteArray();
		}
		
		public GameStateBroadcast(GameState state){
			this(toBytes(state));
		}
		
		public DataInputStream open(){
			return new DataInputStream(new ByteArrayInputStream(data));
		}
	}
	
	record SignalReady() implements NetMessage, Serializable{}
	
	record PlayerMovement(float movement) implements NetMessage, Serializable{}
	
	default boolean execute(ActionSocket socket) throws IOException{
		return false;
	}
	
}
