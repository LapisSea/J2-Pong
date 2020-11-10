package com.lapissea.j2.pong.engine.net;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ActionSocket{
	
	private final Socket           socket;
	private final DataInputStream  in;
	private final DataOutputStream out;
	
	public ActionSocket(Socket socket) throws IOException{
		this.socket=socket;
		in=new DataInputStream(socket.getInputStream());
		out=new DataOutputStream(socket.getOutputStream());
	}
	
	public NetMessage read() throws IOException{
		if(isClosed()) throw new IOException("closed");
		
		try(var dest=new ObjectInputStream(new ByteArrayInputStream(in.readNBytes(in.readInt())))){
			return (NetMessage)dest.readObject();
		}catch(ClassNotFoundException e){
			throw new RuntimeException(e);
		}
	}
	
	public void write(NetMessage message) throws IOException{
		if(isClosed()) throw new IOException("closed");
		
		ByteArrayOutputStream buff=new ByteArrayOutputStream();
		try(var dest=new ObjectOutputStream(buff)){
			dest.writeObject(message);
		}
		
		synchronized(out){
			out.writeInt(buff.size());
			buff.writeTo(out);
		}
	}
	
	public boolean isClosed(){
		return socket.isClosed();
	}
	
	public void close() throws IOException{
		try{
			socket.close();
		}catch(SocketException e){
		
		}
	}
}
