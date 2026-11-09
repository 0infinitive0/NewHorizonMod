package newhorizon.expand.block.supply;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.EnumSet;
import arc.util.Time;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;

/**
 * A passive wind generator: no fuel and no weather, it produces power all the time, swinging slowly between {@link #minOutput} and full
 * output as the wind rises and falls. The wind is a smooth function of the game's clock and of the turbine's own id (so two turbines
 * side by side are not in step, and every player sees the same wind), and the rotor turns faster the harder it blows.
 */
public class WindGenerator extends PowerGenerator{
	/** The least a turbine makes, as a share of {@link #powerProduction}. */
	public float minOutput = 0.25f;
	/** Ticks of the slow swing (the long wind) and of the gusts on top of it. */
	public float swingTicks = 60f * 60f, gustTicks = 60f * 17f;
	/** Rotor turn, in degrees a tick, at full output. */
	public float rotorSpeed = 7f;

	public static final Stat wind = new Stat("nh-wind-range", StatCat.power);

	public TextureRegion rotorRegion;

	public WindGenerator(String name){
		super(name);
		//Like the solar panel, not a priority target for enemies (no generator flag).
		flags = EnumSet.of();
		envEnabled = Env.any;
		buildType = WindGeneratorBuild::new;
	}

	@Override
	public void load(){
		super.load();
		rotorRegion = Core.atlas.find(name + "-rotor");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.remove(generationType);
		stats.add(generationType, powerProduction * 60f, StatUnit.powerSecond);
		stats.add(wind, Core.bundle.format("stat.nh-wind-range.value", (int)(powerProduction * minOutput * 60f), (int)(powerProduction * 60f), (int)(swingTicks / 60f)));
	}

	/** The wind at a moment for a turbine with this id, from 0 to 1. */
	public float windAt(float time, int id){
		float phase = id * 1.37f;
		float swing = 0.5f + 0.5f * Mathf.sin(time / swingTicks * Mathf.PI2 + phase);
		float gust = 0.5f + 0.5f * Mathf.sin(time / gustTicks * Mathf.PI2 + phase * 1.9f + 1.3f);
		return Mathf.clamp(0.7f * swing + 0.3f * gust);
	}

	public class WindGeneratorBuild extends GeneratorBuild{
		public float rotor;

		@Override
		public void updateTile(){
			productionEfficiency = enabled ? minOutput + (1f - minOutput) * windAt(Time.time, id) : 0f;
			rotor += productionEfficiency * rotorSpeed * Time.delta;
		}

		@Override
		public void draw(){
			super.draw();
			if(rotorRegion != null && rotorRegion.found()){
				Draw.z(Layer.block + 0.2f);
				Drawf.spinSprite(rotorRegion, x, y, rotor);
				Draw.reset();
			}
		}
	}
}
