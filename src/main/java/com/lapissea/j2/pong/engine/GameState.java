package com.lapissea.j2.pong.engine;

import com.lapissea.util.MathUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.Vec2f;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GameState{
	
	
	private static final float THRESHOLD=0.00001F;
	
	private long clientId=-1;
	
	private Player playerLeft;
	private Player playerRight;
	
	private final Ball ball=new Ball();
	
	public final BooleanProperty canRun   =new SimpleBooleanProperty();
	public final BooleanProperty isRunning=new SimpleBooleanProperty();
	
	private final List<Runnable> changeListeners=new ArrayList<>();
	
	private final Runnable onReady;
	private final Runnable onWin;
	
	public final FloatProperty playerSize =new SimpleFloatProperty(0.15F);
	public final FloatProperty ballSpeed  =new SimpleFloatProperty(0.5F);
	public final FloatProperty playerSpeed=new SimpleFloatProperty(0.7F);
	
	private long lastUpdateTime;
	
	private final int tps;
	
	public GameState(Runnable onReady, Runnable onWin, int tps){
		this.onReady=onReady;
		this.onWin=onWin;
		this.tps=tps;
		
		reset();
		
		canRun.addListener(e->unready());
	}
	
	private void unready(){
		getPlayerRight().ifPresent(pl->playerReady(pl.getProfileId(), false));
		getPlayerLeft().ifPresent(pl->playerReady(pl.getProfileId(), false));
	}
	
	public void run(){
		while(true){
			UtilL.sleepWhile(()->isRunning.get()&&(System.currentTimeMillis()-lastUpdateTime)<(1000/tps));
			lastUpdateTime=System.currentTimeMillis();
			if(!isRunning.get()) return;
			
			update(1F/tps, 0);
			notifyChange();
		}
	}
	
	private final Vec2f deltaBallSpeed=new Vec2f();
	private final Vec2f newPos        =new Vec2f();
	
	private void update(float delta, int safe){
		
		var pos  =ball.pos;
		var speed=ball.speed;
		
		deltaBallSpeed.set(speed).mul(delta).mul(ballSpeed.get());
		newPos.set(pos).add(deltaBallSpeed);
		
		if(safe>10){//TODO
			throw new StackOverflowError();
		}
		
		if(newPos.x()+THRESHOLD<0){
			if(pos.x()==1){
				collidePlayer(getPlayerRight().orElseThrow(), pos.y());
				speed.set(Math.abs(speed.x()), speed.y());
				update(delta, safe+1);
				return;
			}
			
			float ratio=Math.abs(pos.x()/deltaBallSpeed.x());
			
			update(ratio*delta, safe+1);
			speed.set(Math.abs(speed.x()), speed.y());
//				pos.set(0, pos.y());
			
			collidePlayer(getPlayerLeft().orElseThrow(), pos.y());
			
			update((1-ratio)*delta, safe+1);
			return;
		}
		if(newPos.y()+THRESHOLD<0){
			if(pos.y()==1){
				update(delta, safe+1);
				speed.set(speed.x(), Math.abs(speed.y()));
				return;
			}
			float ratio=Math.abs(pos.y()/deltaBallSpeed.y());
			
			update(ratio*delta, safe+1);
			speed.set(speed.x(), Math.abs(speed.y()));
//				pos.set(pos.x(), 0);
			
			update((1-ratio)*delta, safe+1);
			return;
		}
		
		if(newPos.x()-THRESHOLD>1){
			if(pos.x()==1){
				collidePlayer(getPlayerRight().orElseThrow(), pos.y());
				speed.set(-Math.abs(speed.x()), speed.y());
				update(delta, safe+1);
				return;
			}
			
			float ratio=Math.abs((pos.x()-1)/deltaBallSpeed.x());
			
			update(ratio*delta, safe+1);
			speed.set(-Math.abs(speed.x()), speed.y());
//				pos.set(1, pos.y());
			
			collidePlayer(getPlayerRight().orElseThrow(), pos.y());
			
			update((1-ratio)*delta, safe+1);
			return;
		}
		if(newPos.y()-THRESHOLD>1){
			if(pos.y()==1){
				update(delta, safe+1);
				speed.set(speed.x(), -Math.abs(speed.y()));
				return;
			}
			
			float ratio=Math.abs((pos.y()-1)/deltaBallSpeed.y());
			
			update(ratio*delta, safe+1);
			speed.set(speed.x(), -Math.abs(speed.y()));
//				pos.set(pos.x(), 1);
			
			update((1-ratio)*delta, safe+1);
			return;
		}
		
		pos.add(deltaBallSpeed);
		
		
		updatePlayer(getPlayerLeft().orElseThrow(), delta);
		updatePlayer(getPlayerRight().orElseThrow(), delta);
	}
	
	private void updatePlayer(Player player, float delta){
		player.setPos(MathUtil.snap(player.getPos()+player.getMovement()*delta*playerSpeed.get(), 0, 1));
	}
	
	private void collidePlayer(Player player, float y){
		var pos=player.getPos();
		var siz=playerSize.get()/2;
		
		var midDist=Math.abs(y-pos);
		if(midDist>siz){
			playerLost(player);
			return;
		}
		
		float bounceFac=(float)Math.pow((midDist/siz), 2);
		
		var newSpeed=new Vec2f(1.3F-bounceFac, y>pos?bounceFac:-bounceFac);
		ball.speed.set(newSpeed.div((float)newSpeed.length()));
	}
	
	private void playerLost(Player player){
		ball.speed.set(0, 0);
		ball.pos.set(0.5F, 0.5F);
		
		playerStream().filter(e->e.getProfileId()!=player.getProfileId()).findAny().ifPresent(Player::countWin);
		unready();
		onWin.run();
	}
	
	public void read(DataInputStream src) throws IOException{
		playerLeft=Player.read(src);
		playerRight=Player.read(src);
		
		getPlayerLeft().ifPresent(p->p.movement.addListener(e->notifyChange()));
		getPlayerRight().ifPresent(p->p.movement.addListener(e->notifyChange()));
		
		ball.read(src);
		
		playerSize.set(src.readFloat());
		ballSpeed.set(src.readFloat());
		playerSpeed.set(src.readFloat());
		
		updateCanRun();
		updateIsRunning();
		
		notifyChange();
	}
	
	public void write(DataOutputStream dest) throws IOException{
		Player.write(dest, playerLeft);
		Player.write(dest, playerRight);
		
		ball.write(dest);
		
		dest.writeFloat(playerSize.get());
		dest.writeFloat(ballSpeed.get());
		dest.writeFloat(playerSpeed.get());
	}
	
	public void playerJoined(long profileId){
		if(playerLeft==null){
			playerLeft=new Player(profileId);
			playerLeft.movement.addListener(e->notifyChange());
		}else if(playerRight==null){
			playerRight=new Player(profileId);
			playerRight.movement.addListener(e->notifyChange());
		}
		updateCanRun();
	}
	
	public void playerReady(long profileId, boolean ready){
		if(profileId==-1) throw new IllegalStateException();
		var pl=getPlayer(profileId);
		if(pl.isReady()==ready) return;
		
		pl.setReady(ready);
		
		updateIsRunning();
		notifyChange();
	}
	
	private void updateIsRunning(){
		boolean playing=getPlayerLeft().map(Player::isReady).orElse(false)&&
		                getPlayerRight().map(Player::isReady).orElse(false);
		if(isRunning.get()!=playing) isRunning.set(playing);
	}
	
	public void playerLeft(Profile profile){
		if(profile==null) return;
		
		if(playerLeft!=null&&playerLeft.getProfileId()==profile.id()) playerLeft=null;
		if(playerRight!=null&&playerRight.getProfileId()==profile.id()) playerRight=null;
		
		if(isRunning.get()) isRunning.set(false);
		updateCanRun();
	}
	
	private void updateCanRun(){
		boolean newVal=playerLeft!=null&&playerRight!=null;
		if(!newVal){
			reset();
		}
		if(canRun.get()!=newVal) canRun.set(newVal);
		notifyChange();
	}
	
	public void listenChange(Runnable listener){
		changeListeners.add(listener);
	}
	
	public void notifyChange(){
		for(Runnable listener : changeListeners){
			listener.run();
		}
	}
	
	public void reset(){
		getPlayerLeft().ifPresent(Player::reset);
		getPlayerRight().ifPresent(Player::reset);
		ball.reset();
	}
	
	public Ball getBall(){
		return ball;
	}
	
	public float getPlayerLeftPos(){
		return getPlayerLeft().map(Player::getPos).orElse(0.5F);
	}
	
	public float getPlayerRightPos(){
		return getPlayerRight().map(Player::getPos).orElse(0.5F);
	}
	
	public Optional<Player> getPlayerLeft(){
		return Optional.ofNullable(playerLeft);
	}
	
	public Optional<Player> getPlayerRight(){
		return Optional.ofNullable(playerRight);
	}
	
	public void initClientId(long clientId){
		this.clientId=clientId;
	}
	
	public void signalReady(){
		onReady.run();
		notifyChange();
	}
	
	public Player getThisPlayer(){
		return getPlayer(clientId);
	}
	
	public Stream<Player> playerStream(){
		return Stream.of(getPlayerLeft(), getPlayerRight()).filter(Optional::isPresent).map(Optional::get);
	}
	
	public Player getPlayer(long profileId){
		return playerStream().filter(e->e.getProfileId()==profileId).findAny().orElseThrow();
	}
	
}
