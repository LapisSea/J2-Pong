package com.lapissea.j2.pong.game;

import java.io.Serializable;

public record Message(int profileId, String text) implements Serializable{
}
