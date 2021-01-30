package com.lapissea.j2.pong.engine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;

public record Profile(String userName, BufferedImage icon, long id){
	
	public Profile{
		Objects.requireNonNull(icon);
	}
	
	public static class Ball implements Externalizable{
		
		private Profile value;
		
		public Ball(Profile value){
			this.value=value;
		}
		
		public Ball(){
		}
		
		public Profile get(){
			return value;
		}
		
		@Override
		public void writeExternal(ObjectOutput out) throws IOException{
			out.writeBoolean(value!=null);
			if(value!=null){
				out.writeUTF(value.userName);
				writeImg(out, value.icon);
				out.writeLong(value.id);
			}
		}
		
		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException{
			if(in.readBoolean()){
				value=new Profile(in.readUTF(), readImg(in), in.readLong());
			}
		}
	}
	
	
	private static BufferedImage readImg(ObjectInput in) throws IOException, ClassNotFoundException{
		return ImageIO.read(new ByteArrayInputStream((byte[])in.readObject()));
	}
	
	private static void writeImg(ObjectOutput dest, BufferedImage icon) throws IOException{
		ByteArrayOutputStream buff=new ByteArrayOutputStream();
		ImageIO.write(icon, "PNG", buff);
		dest.writeObject(buff.toByteArray());
	}
	
	
	@Override
	public String toString(){
		return "Profile{"+
		       "userName='"+userName+'\''+
		       ", id="+id+
		       '}';
	}
	
	public Profile withIcon(BufferedImage icon){
		return new Profile(userName, icon, id);
	}
	
	public Profile withId(long id){
		return new Profile(userName, icon, id);
	}
}
