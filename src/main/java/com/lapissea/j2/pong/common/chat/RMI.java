package com.lapissea.j2.pong.common.chat;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMI{
	
	
	public static ChatService connectService(int port){
		try{
			return (ChatService)LocateRegistry.getRegistry(port).lookup(ChatService.class.getName());
		}catch(RemoteException|NotBoundException e){
			throw new RuntimeException(e);
		}
	}
	
	public static void makeService(int port, ChatService remoteService){
		try{
			Registry    registry=LocateRegistry.createRegistry(port);
			ChatService serv    =(ChatService)UnicastRemoteObject.exportObject(remoteService, port);
			registry.rebind(ChatService.class.getName(), serv);
		}catch(RemoteException e){
			throw new RuntimeException(e);
		}
	}
}
