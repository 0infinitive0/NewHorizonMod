package newhorizon.expand.block.nuke;

import arc.math.geom.Vec2;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeStatus;

/**
 * A Nuke Launcher that throws its nuke up to the edge of space: it holds one nuke at a time like the plain launcher, but the nuke it
 * sends flies so high that nothing can target it or shoot it down on the way, so it reaches its target whatever defends it.
 */
public class OrbitalLauncher extends NukeLauncher{
	/** Ticks a launched nuke stays out of reach. Longer than any nuke can fly. */
	public float altitudeTime = 60f * 120f;

	public OrbitalLauncher(String name){
		super(name);
		size = 5;
		buildType = OrbitalLauncherBuild::new;
	}

	public class OrbitalLauncherBuild extends NukeLauncherBuild{
		@Override
		protected void send(Unit nuke, float lateral, Vec2 aim){
			nuke.apply(NukeStatus.highAltitude, altitudeTime);
			super.send(nuke, lateral, aim);
		}
	}
}
