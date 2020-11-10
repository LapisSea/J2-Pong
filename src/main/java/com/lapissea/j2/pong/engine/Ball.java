package com.lapissea.j2.pong.engine;

import com.lapissea.util.Rand;
import com.lapissea.vec.Vec2f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Ball{
	
	public final Vec2f pos  =new Vec2f();
	public final Vec2f speed=new Vec2f();
	
	public Ball(){
		reset();
	}
	
	public void reset(){
		
		pos.set(0.5F, 0.5F);
		double angle=Rand.d(Math.PI*2);
		
		speed.set((float)Math.sin(angle), (float)Math.cos(angle));
	}
	
	public void read(DataInputStream src) throws IOException{
		pos.set(src.readFloat(), src.readFloat());
		speed.set(src.readFloat(), src.readFloat());
	}
	
	public void write(DataOutputStream dest) throws IOException{
		dest.writeFloat(pos.x());
		dest.writeFloat(pos.y());
		dest.writeFloat(speed.x());
		dest.writeFloat(speed.y());
	}
}
