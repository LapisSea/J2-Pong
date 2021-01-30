package com.lapissea.j2.pong.engine;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Player{
	
	public static Player read(DataInputStream src) throws IOException{
		long id=src.readLong();
		if(id==-1) return null;
		
		Player player=new Player(id);
		player.setPos(src.readFloat());
		player.setReady(src.readBoolean());
		player.setMovement(src.readFloat());
		player.wins=src.readInt();
		return player;
	}
	
	public static void write(DataOutputStream dest, Player player) throws IOException{
		if(player==null){
			dest.writeLong(-1);
			return;
		}
		
		dest.writeLong(player.getProfileId());
		dest.writeFloat(player.getPos());
		dest.writeBoolean(player.isReady());
		dest.writeFloat(player.getMovement());
		dest.writeInt(player.wins);
	}
	
	
	private final long    profileId;
	private       boolean ready;
	
	final   FloatProperty movement=new SimpleFloatProperty();
	private int           wins;
	
	private float pos;
	
	public Player(long profileId){
		this.profileId=profileId;
		reset();
	}
	
	public float getPos(){
		return pos;
	}
	
	public void setPos(float pos){
		this.pos=pos;
	}
	
	public long getProfileId(){
		return profileId;
	}
	
	public void reset(){
		pos=0.5F;
		setReady(false);
		setMovement(0);
	}
	
	public int getWins(){
		return wins;
	}
	
	public void countWin(){
		this.wins++;
	}
	
	public float getMovement(){
		return movement.get();
	}
	
	public void setMovement(float movement){
		this.movement.set(movement);
	}
	
	public boolean isReady(){
		return ready;
	}
	
	public void setReady(boolean ready){
		this.ready=ready;
	}
}
