package newhorizon.expand.block.nuke;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.graphics.Layer;
import mindustry.input.Binding;
import newhorizon.expand.units.nuke.NukeDefense;

/**
 * Picking a launcher's target on the map: the launcher's menu closes and the camera is let go of, so it can be panned (the movement
 * keys, or dragging with the mouse or a finger; the wheel zooms as usual). A click or tap that was not a drag sets the target; escape or
 * a right click cancels. Only one launcher waits at a time.
 */
public class NukeTargeting{
	private static NukeLauncher.NukeLauncherBuild pending;
	private static long startFrame;
	private static boolean hooked;
	/** A press that may turn out to be a drag, and where it began on the screen. */
	private static boolean touching, dragged;
	private static float downX, downY;

	/** How far a press has to move on the screen before it counts as a drag, in pixels. */
	private static final float dragThreshold = 12f;

	public static void begin(NukeLauncher.NukeLauncherBuild launcher){
		if(Vars.headless) return;
		if(!hooked){
			hooked = true;
			Events.run(EventType.Trigger.update, NukeTargeting::tick);
			Events.run(EventType.Trigger.draw, NukeTargeting::draw);
			Events.on(EventType.ResetEvent.class, e -> finish());
		}
		//Locks the player's own controls while a target is being picked: the movement keys pan the camera instead of moving the unit, and the
		//camera stops following the unit.
		if(!Vars.control.input.inputLocks.contains(lock)) Vars.control.input.inputLocks.add(lock);
		pending = launcher;
		touching = dragged = false;
		startFrame = Core.graphics.getFrameId();
		Vars.control.input.config.hideConfig();
		Vars.ui.showInfoToast(Core.bundle.get("nh.nuke.pick"), 5f);
	}

	public static boolean waiting(){
		return pending != null;
	}

	/** Held by the input handler while a pick is under way. */
	private static final arc.func.Boolp lock = () -> pending != null;

	/** Ends the pick and gives the camera back. */
	private static void finish(){
		pending = null;
		touching = dragged = false;
	}

	private static void tick(){
		if(pending == null) return;
		if(!pending.isValid() || !Vars.state.isGame()){
			finish();
			return;
		}
		if(Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.mouseRight)){
			finish();
			return;
		}

		if(!Core.scene.hasField() && !Core.scene.hasKeyboard()){
			float speed = (Core.input.keyDown(Binding.boost) ? 14f : 7f) * Time.delta;
			float dx = Core.input.axis(Binding.moveX), dy = Core.input.axis(Binding.moveY);
			if(dx != 0f || dy != 0f) Core.camera.position.add(Tmp.v1.set(dx, dy).nor().scl(speed));
		}

		if(Core.input.justTouched() && Core.graphics.getFrameId() > startFrame && !Core.scene.hasMouse()){
			touching = true;
			dragged = false;
			downX = Core.input.mouseX();
			downY = Core.input.mouseY();
		}
		if(!touching) return;

		if(Core.input.isTouched()){
			if(!dragged && Mathf.dst(Core.input.mouseX(), Core.input.mouseY(), downX, downY) > dragThreshold) dragged = true;
			if(dragged){
				//The map follows the finger: the camera goes the other way.
				float scale = Core.camera.width / Core.graphics.getWidth();
				Core.camera.position.add(-Core.input.deltaX() * scale, -Core.input.deltaY() * scale);
			}
		}else{
			touching = false;
			if(!dragged && !Core.scene.hasMouse()){
				Vec2 world = Core.input.mouseWorld();
				if(NukeDefense.cloaked(pending.team, world.x, world.y)){
					//Hidden by an enemy cloak: it cannot be targeted, keep picking.
					Vars.ui.showInfoToast(Core.bundle.get("nh.nuke.cloaked"), 3f);
					return;
				}
				pending.configure(new Vec2(world.x, world.y));
				finish();
			}
		}
	}

	/** A cross and a line from the launcher at the cursor, so it is clear where the nuke would go. */
	private static void draw(){
		if(pending == null || !pending.isValid() || touching && dragged) return;
		Vec2 world = Core.input.mouseWorld();
		Draw.z(Layer.overlayUI);
		Draw.color(NukeDefense.cloaked(pending.team, world.x, world.y) ? Color.gray : Color.scarlet, 0.8f);
		Lines.stroke(1.5f);
		Lines.dashLine(pending.x, pending.y, world.x, world.y, (int)(pending.dst(world.x, world.y) / 14f));
		Lines.circle(world.x, world.y, 14f);
		Lines.line(world.x - 20f, world.y, world.x + 20f, world.y);
		Lines.line(world.x, world.y - 20f, world.x, world.y + 20f);
		Draw.reset();
	}
}
