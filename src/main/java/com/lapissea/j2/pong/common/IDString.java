package com.lapissea.j2.pong.common;

import java.io.Serializable;
import java.util.Objects;

public class IDString implements Serializable{
	
	public int    id;
	public String text;
	
	
	public IDString(){
	
	}
	
	public IDString(int id, String text){
		this.id=id;
		this.text=text;
	}
	
	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		return o instanceof IDString idString&&
		       id==idString.id&&
		       Objects.equals(text, idString.text);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(id, text);
	}
}
