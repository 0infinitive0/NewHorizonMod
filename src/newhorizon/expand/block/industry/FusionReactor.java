package newhorizon.expand.block.industry;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import newhorizon.content.industry.IndustryLiquids;
import mindustry.ui.Bar;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

/**
 * The Fusion Reactor. It runs on hydrogen (full output) or, less well, on ozone (half the output, burnt twice as fast), and gives off two
 * liquids as byproducts: super hot <b>plasma</b> out of its left side and <b>anti-matter</b> out of its right side (relative to its rotation;
 * the anti-matter explodes if it leaks, see {@code AntiMatterLiquid}). Cryofluid, if it is supplied, lifts the output by half.
 *
 * <p>It has to be <b>ignited</b> first: while it holds fuel and is not lit it draws a very large amount of power from the grid (its
 * power use, for {@link #igniteTicks} at full supply, slower on a weaker grid), and does not generate. Once lit it stops drawing,
 * generates, and stays lit while fuel keeps coming. When the fuel runs out the plasma dies away over {@link #cooldownTicks}, and it has to
 * be ignited again. It never ignites without fuel.
 *
 * <p>If either byproduct has nowhere to go and its buffer is full, it stalls (it stops burning) until some is taken away.
 */
public class FusionReactor extends PowerGenerator{
	/** Ticks of full power supply to ignite, and ticks for the plasma to die away with no fuel. */
	public float igniteTicks = 1200f, cooldownTicks = 600f;
	/** Fuel burnt a tick, and the output of ozone against hydrogen. */
	public float hydrogenUse = 24f / 60f, ozoneUse = 48f / 60f, ozoneOutput = 0.5f;
	/** Cryofluid used a tick when it is supplied, and how much the output rises with it. */
	public float cryoUse = 6f / 60f, cryoBoost = 1.5f;
	/** Plasma and anti-matter made a tick at full burn, and how much of each it holds before it stalls. */
	public float plasmaOut = 12f / 60f, antiMatterOut = 0.6f / 60f, byproductCap = 600f;
	/** Ticks for the output to come up to full once lit. */
	public float rampTicks = 120f;
	public Color hydrogenColor = Color.valueOf("7ff4ff"), ozoneColor = Color.valueOf("b45cff"), boostColor = Color.valueOf("c9f4ff");

	/** The sides the byproducts leave from, relative to the rotation (1 is the left, 3 the right). */
	public static final int plasmaSide = 1, antiMatterSide = 3;

	public static final Stat modes = new Stat("nh-fusion-modes", StatCat.power);

	public TextureRegion bottomRegion, lightsRegion, plasmaPortRegion, antiPortRegion;

	public FusionReactor(String name){
		super(name);
		hasLiquids = true;
		//Turnable: the plasma leaves from the left side and the anti-matter from the right, like the Electric Separator's liquids.
		rotate = true;
		outputsPower = true;
		//A generator is not a consumer of the power graph unless it says so, and then its ignition power is never delivered (efficiency stays 0).
		consumesPower = true;
		liquidCapacity = 1200f;
		explosionDamage = 0;
		explosionRadius = 0;
		buildType = FusionReactorBuild::new;
	}

	@Override
	public void load(){
		super.load();
		bottomRegion = Core.atlas.find(name + "-bottom");
		lightsRegion = Core.atlas.find(name + "-lights");
		plasmaPortRegion = Core.atlas.find(name + "-plasma-port");
		antiPortRegion = Core.atlas.find(name + "-antimatter-port");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(modes, Core.bundle.format("stat.nh-fusion-modes.value", (int)powerProduction, (int)(powerProduction * ozoneOutput), (int)(hydrogenUse * 60f), (int)(ozoneUse * 60f),
			(int)(cryoBoost * 100f - 100f), (int)(cryoUse * 60f), (int)(igniteTicks / 60f), Strings.autoFixed(plasmaOut * 60f, 1), Strings.autoFixed(antiMatterOut * 60f, 1)));
	}

	@Override
	public void setBars(){
		super.setBars();
		//How much of the ignition power the grid actually delivers (the generator's own bar shows only what it makes).
		addBar("nh-fusion-supply", (FusionReactorBuild b) -> new Bar(
			() -> Core.bundle.format("bar.nh-fusion-supply", (int)(b.power.status * 100f)),
			() -> Pal.powerBar,
			() -> b.power.status));
		addBar("nh-fusion-ignition", (FusionReactorBuild b) -> new Bar(
			() -> Core.bundle.format("bar.nh-fusion-ignition", (int)(b.ignition * 100f), b.lit ? Core.bundle.get("bar.nh-fusion-lit") : ""),
			() -> b.lit ? Pal.accent : Pal.lighterOrange,
			() -> b.ignition));
		addBar("nh-fusion-fuel", (FusionReactorBuild b) -> new Bar(
			() -> {
				Liquid f = b.fuel();
				return f == null ? Core.bundle.get("bar.nh-fusion-nofuel") : f.localizedName;
			},
			() -> b.fuel() == Liquids.ozone ? ozoneColor : hydrogenColor,
			() -> b.fuel() == null ? 0f : b.liquids.get(b.fuel()) / liquidCapacity));
		addBar("nh-fusion-cryo", (FusionReactorBuild b) -> new Bar(
			() -> Liquids.cryofluid.localizedName,
			() -> boostColor,
			() -> b.liquids.get(Liquids.cryofluid) / liquidCapacity));
		addBar("nh-fusion-plasma", (FusionReactorBuild b) -> new Bar(
			() -> IndustryLiquids.plasma.localizedName,
			() -> IndustryLiquids.plasma.barColor(),
			() -> b.liquids.get(IndustryLiquids.plasma) / byproductCap));
		addBar("nh-fusion-antimatter", (FusionReactorBuild b) -> new Bar(
			() -> IndustryLiquids.antiMatter.localizedName,
			() -> IndustryLiquids.antiMatter.barColor(),
			() -> b.liquids.get(IndustryLiquids.antiMatter) / byproductCap));
	}

	public class FusionReactorBuild extends GeneratorBuild{
		/** How far the plasma is lit (0 to 1), whether it is lit, the output coming up, and the cryofluid boost. */
		public float ignition, output, boost;
		public boolean lit;
		protected Liquid lastFuel = Liquids.hydrogen;
		protected float clock;

		/** The fuel it would burn now: hydrogen first, then ozone, or null. */
		public Liquid fuel(){
			if(liquids.get(Liquids.hydrogen) > 0.01f) return Liquids.hydrogen;
			if(liquids.get(Liquids.ozone) > 0.01f) return Liquids.ozone;
			return null;
		}

		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return (liquid == Liquids.hydrogen || liquid == Liquids.ozone || liquid == Liquids.cryofluid) && liquids.get(liquid) < liquidCapacity - 0.001f;
		}

		/** It asks the grid for the ignition power only while it holds fuel and is not lit. */
		@Override
		public boolean shouldConsume(){
			return !lit && fuel() != null;
		}

		@Override
		public float warmup(){
			return output;
		}

		@Override
		public void updateTile(){
			float dt = delta();
			Liquid f = fuel();
			if(f != null) lastFuel = f;

			//Lighting it, and letting it go out.
			if(!lit){
				if(f != null){
					ignition += efficiency * dt / igniteTicks;
					if(ignition >= 1f){
						ignition = 1f;
						lit = true;
					}
				}else{
					ignition = Math.max(0f, ignition - dt / cooldownTicks);
				}
			}else if(f == null){
				ignition -= dt / cooldownTicks;
				if(ignition <= 0f){
					ignition = 0f;
					lit = false;
				}
			}else{
				ignition = 1f;
			}

			//Burning.
			boolean blocked = liquids.get(IndustryLiquids.plasma) >= byproductCap - 0.001f || liquids.get(IndustryLiquids.antiMatter) >= byproductCap - 0.001f;
			float target = 0f, boostTarget = 0f;
			if(lit && f != null && !blocked){
				float use = (f == Liquids.hydrogen ? hydrogenUse : ozoneUse) * dt;
				float have = liquids.get(f);
				float share = use <= 0f ? 1f : Math.min(1f, have / use);
				liquids.remove(f, Math.min(use, have));
				target = (f == Liquids.hydrogen ? 1f : ozoneOutput) * share;
				float cryo = cryoUse * dt;
				if(liquids.get(Liquids.cryofluid) >= cryo){
					liquids.remove(Liquids.cryofluid, cryo);
					boostTarget = 1f;
				}
			}
			output = Mathf.approachDelta(output, target, 1f / rampTicks);
			boost = Mathf.approachDelta(boost, boostTarget, 1f / 60f);
			productionEfficiency = output * (1f + (cryoBoost - 1f) * boost);

			//The byproducts, in proportion to the burn, each out of its own side (relative to the rotation: 1 is the left, 3 the right).
			if(output > 0.001f){
				liquids.add(IndustryLiquids.plasma, Math.min(plasmaOut * output * dt, byproductCap - liquids.get(IndustryLiquids.plasma)));
				liquids.add(IndustryLiquids.antiMatter, Math.min(antiMatterOut * output * dt, byproductCap - liquids.get(IndustryLiquids.antiMatter)));
			}
			if(liquids.get(IndustryLiquids.plasma) > 0.001f) dumpLiquid(IndustryLiquids.plasma, 2f, plasmaSide);
			if(liquids.get(IndustryLiquids.antiMatter) > 0.001f) dumpLiquid(IndustryLiquids.antiMatter, 2f, antiMatterSide);
			clock += dt * (0.4f + 2.2f * output);
		}

		protected Color plasma(){
			Color base = lastFuel == Liquids.ozone ? ozoneColor : hydrogenColor;
			return boost > 0.01f ? base.cpy().lerp(boostColor, boost * 0.6f) : base;
		}

		@Override
		public void draw(){
			Draw.rect(bottomRegion.found() ? bottomRegion : region, x, y);
			float glow = Mathf.clamp(ignition * 0.3f + output * 0.7f);
			float r = size * 8f * 0.5f;
			if(glow > 0.01f){
				Color c = plasma();
				Draw.z(Layer.block + 0.2f);
				float pulse = 1f + 0.05f * Mathf.sin(Time.time * 0.1f);
				Draw.color(c, 0.18f * glow);
				Fill.circle(x, y, r * 0.62f * pulse);
				Draw.color(c, 0.35f * glow);
				Fill.circle(x, y, r * 0.42f * pulse);
				//Rotating arcs of plasma, faster the harder it runs.
				Lines.stroke(2.2f * glow + 0.4f);
				for(int i = 0; i < 3; i++){
					Draw.color(c, (0.55f + 0.1f * i) * glow);
					Lines.arc(x, y, r * (0.32f + 0.12f * i), 0.28f, clock * (i % 2 == 0 ? 1f : -1.3f) * 3f + i * 120f);
				}
				Draw.color(Color.white, c, 0.35f);
				Draw.alpha(glow);
				Fill.circle(x, y, r * 0.16f * (0.8f + 0.4f * output));
				Draw.reset();
			}
			Draw.z(Layer.block + 0.3f);
			Draw.rect(region, x, y);
			//The ports of the two byproducts, turned with the block.
			if(plasmaPortRegion.found()) Draw.rect(plasmaPortRegion, x, y, rotdeg());
			if(antiPortRegion.found()) Draw.rect(antiPortRegion, x, y, rotdeg());
			if(lightsRegion.found()){
				Draw.color(plasma());
				Draw.alpha(0.25f + 0.75f * glow);
				Draw.rect(lightsRegion, x, y);
				Draw.reset();
			}
		}

		@Override
		public void drawLight(){
			float glow = Mathf.clamp(ignition * 0.3f + output * 0.7f);
			if(glow > 0.01f) Drawf.light(x, y, size * 8f * (1.6f + 1.4f * output), plasma(), 0.5f * glow);
		}

		@Override
		public byte version(){
			return 2;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(ignition);
			write.bool(lit);
			write.f(output);
			write.f(boost);
			write.b(lastFuel == Liquids.ozone ? 1 : 0);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision >= 1){
				ignition = read.f();
				lit = read.bool();
				output = read.f();
				boost = read.f();
				//Revision 1 also kept the timer of the thorium it used to make.
				if(revision < 2) read.f();
				lastFuel = read.b() == 1 ? Liquids.ozone : Liquids.hydrogen;
			}
		}
	}
}
