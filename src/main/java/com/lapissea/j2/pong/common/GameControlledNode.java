package com.lapissea.j2.pong.common;

import com.lapissea.j2.pong.game.client.elements.GameStageController;
import javafx.scene.layout.Region;

public record GameControlledNode(Region node, GameStageController controller){}
