package newhorizon.expand.block.nuke;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.game.EventType;

/**
 * Picking a launcher's target on the map: the launcher's menu closes, the next click on the world sets the target, and escape or a
 * right click cancels. Only one launcher waits at a time.
 */
public class NukeTargeting{
	private static NukeLauncher.NukeLauncherBuild pending;
	private static long startFrame;
	private static boolean hooked;

	public static void begin(NukeLauncher.NukeLauncherBuild launcher){
		if(Vars.headless) return;
		if(!hooked){
			hooked = true;
			Events.run(EventType.Trigger.update, NukeTargeting::tick);
			Events.on(EventType.ResetEvent.class, e -> pending = null);
		}
		pending = launcher;
		startFrame = Core.graphics.getFrameId();
		Vars.control.input.config.hideConfig();
		Vars.ui.showInfoToast(Core.bundle.get("nh.nuke.pick"), 4f);
	}

	public static boolean waiting(){
		return pending != null;
	}

	private static void tick(){
		if(pending == null) return;
		if(!pending.isValid() || !Vars.state.isGame()){
			pending = null;
			return;
		}
		if(Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.mouseRight)){
			pending = null;
		}else if(Core.input.justTouched() && Core.graphics.getFrameId() > startFrame && !Core.scene.hasMouse()){
			Vec2 world = Core.input.mouseWorld();
			pending.configure(new Vec2(world.x, world.y));
			pending = null;
		}
	}
}
