package com.lapissea.j2.pong.engine;

import com.lapissea.util.TextUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Status{
	
	RESULT,
	WAITING_PLAYERS,
	WAITING_START,
	RUNNING;
	
	public final String stageName;
	
	Status(){
		stageName=Arrays.stream(this.name().split("_")).map(s->TextUtil.firstToUpperCase(s.toLowerCase())).collect(Collectors.joining());
	}
}
