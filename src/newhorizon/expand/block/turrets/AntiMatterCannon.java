package newhorizon.expand.block.turrets;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Position;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.NukeFx;
import newhorizon.expand.units.nuke.AntiMatter;

import static mindustry.Vars.tilesize;

/**
 * A turret that cannot aim by itself: it does nothing for an AI or with nobody at its controls. Someone who has taken control of it (a
 * player, or a logic processor that gives it a shoot order for a point) <b>holds fire to charge it</b>; it does not keep the charge, which drains away when fire is let go (or the power fails). With the charge
 * full, the barrel on the cursor and a target of another team under it, it fires one beam. What the beam hits is destroyed outright, and the
 * destruction spreads (see {@link AntiMatter}); a shield in the way is destroyed instead, and nothing spreads. After a shot the cannon cools
 * for a while and cannot be charged. The state is drawn: coils that pulse towards the muzzle as it charges, a core that swells, a flare and
 * recoil when it fires, glow that fades as it powers down, and a hot barrel with vents as it cools. (The state is worked out by every side
 * that runs the turret; only the server makes the shot.)
 */
public class AntiMatterCannon extends PowerTurret{
	/** Ticks to charge from empty to full, to lose a full charge when not charging, and to cool after a shot. */
	public float chargeTicks = 60f * 90f, drainTicks = 60f * 120f, coolTicks = 60f * 90f;
	/** How far the muzzle is from the middle of the turret, and how closely the barrel has to be on the cursor to fire. */
	public float muzzleLength = 30f, fireCone = 5f;
	/** The distances of the barrel's coils from the middle, along the barrel. */
	public float[] coils = {14f, 20f, 26f};
	public Color glow = Color.valueOf("7ff4ff"), glow2 = Color.valueOf("b45cff");

	public TextureRegion baseGlowRegion, baseR, outlineR;

	public enum State{
		idle, charging, ready, down, cooling
	}

	public AntiMatterCannon(String name){
		super(name);
		envEnabled = Env.any;
		targetAir = true;
		targetGround = true;
		buildType = AntiMatterBuild::new;
		//The base game's turret code only ever shoots a bullet that does nothing: the real shot is made below.
		reload = 1e9f;
		shootCone = 360f;
		recoil = 0f;
	}

	@Override
	public void load(){
		super.load();
		baseGlowRegion = Core.atlas.find(name + "-base-glow");
		baseR = Core.atlas.find(name + "-base");
		outlineR = Core.atlas.find(name + "-outline");
	}

	@Override
	public void init(){
		super.init();
		AntiMatter.hook();
	}

	/** A team can have only one: not while one stands, and not while one is being built. */
	@Override
	public boolean canPlaceOn(mindustry.world.Tile tile, mindustry.game.Team team, int rotation){
		if(!super.canPlaceOn(tile, team, rotation)) return false;
		mindustry.game.Teams.TeamData data = team.data();
		if(!data.getBuildings(this).isEmpty()) return false;
		for(Building b : data.buildings){
			if(b instanceof mindustry.world.blocks.ConstructBlock.ConstructBuild && ((mindustry.world.blocks.ConstructBlock.ConstructBuild)b).current == this) return false;
		}
		return true;
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-antimatter", (AntiMatterBuild b) -> new Bar(
			() -> Core.bundle.format("nh.antimatter.state." + b.state().name(), (int)(b.energy * 100f), (int)(b.cool / 60f)),
			() -> b.state() == State.cooling ? Pal.lightishOrange : b.state() == State.down ? Pal.accent : Color.valueOf("b45cff"),
			() -> b.state() == State.cooling ? 1f - b.cool / coolTicks : b.energy
		));
	}

	public class AntiMatterBuild extends PowerTurretBuild{
		/** How full the charge is (0 to 1), and the ticks of cooling left after a shot. */
		public float energy, cool;
		/** Whether the pilot held fire last tick, how far the barrel is kicked back, and a clock for the animations. */
		public boolean holding;
		public float kick, flare, clock, ventTimer;
		private boolean wasFull;
		private float lastEnergy;

		public State state(){
			if(cool > 0f) return State.cooling;
			if(energy >= 1f) return State.ready;
			if(holding && energy > 0f) return State.charging;
			if(holding) return State.charging;
			return energy > 0f ? State.down : State.idle;
		}

		/** No target of its own, ever. */
		@Override
		protected void findTarget(){
			target = null;
		}

		/** Turns to the cursor while piloted: that is all the base turret code is left to do. */
		@Override
		public boolean hasAmmo(){
			return true;
		}

		/** The shot is made by {@link #updateTile}, not by the base game's reload. */
		@Override
		protected void updateShooting(){
		}

		@Override
		public boolean shouldConsume(){
			return holding && cool <= 0f && energy < 1f;
		}

		@Override
		public void updateTile(){
			super.updateTile();
			clock += Time.delta;
			kick = Math.max(0f, kick - Time.delta / 40f);
			flare = Math.max(0f, flare - Time.delta / 24f);
			//Someone is at the controls (a player, or a processor with a shoot order) or nobody is: it never picks a target by itself.
			boolean pilot = isControlled() || logicControlled();
			holding = (isControlled() && unit != null && unit.isShooting()) || (!isControlled() && logicControlled() && logicShooting);

			if(cool > 0f){
				cool = Math.max(0f, cool - Time.delta);
				energy = 0f;
				ventTimer -= Time.delta;
				if(ventTimer <= 0f && !Vars.headless){
					ventTimer = 3f + 10f * (1f - cool / coolTicks);
					float side = Mathf.range(1f);
					Effect vent = NukeFx.antiVent;
					vent.at(x + Angles.trnsx(rotation + 90f, side * 14f) + Angles.trnsx(rotation, Mathf.range(14f) - 6f), y + Angles.trnsy(rotation + 90f, side * 14f) + Angles.trnsy(rotation, Mathf.range(14f) - 6f), rotation + 90f * Mathf.sign(side));
				}
			}else if(holding && efficiency > 0f){
				energy = Math.min(1f, energy + Time.delta * efficiency / chargeTicks);
			}else{
				energy = Math.max(0f, energy - Time.delta / drainTicks);
			}

			if(!wasFull && energy >= 1f){
				Effect.shake(3f, 20f, this);
				if(!Vars.headless) Sounds.chargeLancer.at(x, y, 0.9f, 0.8f);
				flare = 0.7f;
			}
			wasFull = energy >= 1f;
			if(holding && lastEnergy <= 0f && energy > 0f && !Vars.headless) Sounds.chargeVela.at(x, y, 0.9f, 0.7f);
			lastEnergy = energy;

			if(Vars.net.client() || !pilot || !holding || energy < 1f || cool > 0f) return;
			float mx = x + Angles.trnsx(rotation, muzzleLength), my = y + Angles.trnsy(rotation, muzzleLength);
			if(Angles.angleDist(rotation, angleTo(targetPos.x, targetPos.y)) > fireCone) return;
			Position t = AntiMatter.pick(team, targetPos.x, targetPos.y, mx, my, range());
			if(t != null) fire(mx, my, t);
		}

		/** Spends the charge on one beam at the target. */
		public void fire(float mx, float my, Position t){
			float tx = t.getX(), ty = t.getY();
			AntiMatter.Block shield = AntiMatter.shield(team, mx, my, tx, ty, t instanceof Building ? (Building)t : null);
			float ex = shield != null ? shield.x : tx, ey = shield != null ? shield.y : ty;
			float angle = Angles.angle(mx, my, ex, ey), len = Mathf.dst(mx - ex, my - ey);
			Call.effect(NukeFx.antiBeam, mx, my, angle, new Color(Math.min(1f, len / 1020f), 0f, 0f, 1f));
			if(shield != null) AntiMatter.destroyShield(shield);
			else AntiMatter.annihilate(t);

			energy = 0f;
			cool = coolTicks;
			kick = 1f;
			flare = 1f;
			wasFull = false;
			Effect.shake(10f, 40f, this);
			Sounds.beamMeltdown.at(x, y, 1f, 0.6f);
			Sounds.explosion.at(x, y, 1f, 0.5f);
		}

		@Override
		public void draw(){
			Draw.rect(baseR, x, y);
			float lvl = energy, hot = cool > 0f ? cool / coolTicks : 0f;
			//The seams of the base light up with the charge, from nothing to full, and flare when it fires.
			if(baseGlowRegion != null && baseGlowRegion.found()){
				float a = Math.max(lvl, flare) * (0.8f + 0.2f * Mathf.absin(clock, 8f, 1f));
				Draw.z(Layer.block + 0.1f);
				Draw.blend(Blending.additive);
				Draw.color(glow, glow2, 1f - lvl);
				Draw.alpha(a);
				Draw.rect(baseGlowRegion, x, y);
				Draw.blend();
				Draw.color();
			}

			Draw.z(Layer.turret);
			float ox = x - Angles.trnsx(rotation, 10f * kick), oy = y - Angles.trnsy(rotation, 10f * kick);
			Drawf.shadow(region, ox - elevation, oy - elevation, rotation - 90f);
			//The game makes the head's outline picture itself, and it goes under the head.
			if(outlineR != null && outlineR.found()) Draw.rect(outlineR, ox, oy, rotation - 90f);
			Draw.rect(region, ox, oy, rotation - 90f);

			Draw.z(Layer.turret + 0.5f);
			Draw.blend(Blending.additive);
			//Hot barrel while it cools: white, then orange, fading.
			if(hot > 0f){
				Draw.color(Color.white, Color.valueOf("ff7a2d"), 1f - hot);
				Draw.alpha(0.45f * hot);
				Draw.rect(region, ox, oy, rotation - 90f);
			}
			//The coils pulse in turn towards the muzzle, faster the fuller the charge; they hold steady when it is full and fade when it drains.
			for(int i = 0; i < coils.length; i++){
				float wave = state() == State.charging ? 0.5f + 0.5f * Mathf.sin(clock * (0.08f + 0.35f * lvl) - i * 1.1f) : state() == State.ready ? 0.75f + 0.25f * Mathf.absin(clock, 6f, 1f) : 1f;
				float level = state() == State.cooling ? 0f : lvl * wave;
				if(level <= 0.01f) continue;
				float cx = ox + Angles.trnsx(rotation, coils[i]), cy = oy + Angles.trnsy(rotation, coils[i]);
				Draw.color(glow, glow2, 1f - lvl);
				Draw.alpha(level);
				Draw.rect(Core.atlas.white(), cx, cy, 3f, 21f, rotation);
			}
			//The core at the muzzle.
			if(lvl > 0.02f || flare > 0f){
				float pulse = state() == State.ready ? 1f + 0.12f * Mathf.absin(clock, 5f, 1f) : 1f;
				float r = (2f + 9f * lvl) * pulse + 14f * flare;
				float cx = ox + Angles.trnsx(rotation, muzzleLength - 2f), cy = oy + Angles.trnsy(rotation, muzzleLength - 2f);
				Draw.color(glow2, 0.35f + 0.4f * lvl);
				Fill.circle(cx, cy, r * 1.7f);
				Draw.color(glow, 0.6f + 0.4f * lvl);
				Fill.circle(cx, cy, r);
				Draw.color(Color.white, 1f);
				Fill.circle(cx, cy, r * 0.5f);
				Drawf.light(cx, cy, r * 9f, glow, 0.6f * Math.max(lvl, flare));
				//While it charges, sparks are drawn in towards the core, and at full it crackles.
				if(state() == State.charging || state() == State.ready){
					Rand rand = new Rand(id + (long)(clock / 3f));
					Draw.color(glow, 0.8f);
					Lines.stroke(1.2f);
					int n = state() == State.ready ? 5 : 2 + (int)(5 * lvl);
					for(int i = 0; i < n; i++){
						float a = rand.random(360f), d = rand.random(14f, 40f) * (state() == State.charging ? 1f - (clock * 0.05f + i * 0.2f) % 1f * 0.7f : 1f);
						Lines.line(cx + Angles.trnsx(a, d), cy + Angles.trnsy(a, d), cx + Angles.trnsx(a, d * 0.45f), cy + Angles.trnsy(a, d * 0.45f));
					}
				}
			}
			//The visor on the housing.
			if(lvl > 0.02f){
				Draw.color(glow, 0.3f + 0.7f * lvl);
				Draw.rect(Core.atlas.white(), ox + Angles.trnsx(rotation, 1f), oy + Angles.trnsy(rotation, 1f), 3f, 12f * Math.min(1f, 0.4f + lvl), rotation);
			}
			Draw.blend();
			Draw.color();
			Draw.reset();
		}

		/** What a processor can read besides the usual: how full the charge is, and how much of the cooldown after a shot is left. */
		@Override
		public double sense(mindustry.logic.LAccess sensor){
			switch(sensor){
				case progress: return energy;
				case heat: return cool / coolTicks;
				case shooting: return holding ? 1 : 0;
				default: return super.sense(sensor);
			}
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range(), team.color);
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(energy);
			write.f(cool);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			energy = read.f();
			cool = read.f();
		}
	}
}
