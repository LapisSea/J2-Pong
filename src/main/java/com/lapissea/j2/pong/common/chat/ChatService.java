package com.lapissea.j2.pong.common.chat;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatService extends Remote{
	
	void recieveMessage(String message, long profileId) throws RemoteException;
}
