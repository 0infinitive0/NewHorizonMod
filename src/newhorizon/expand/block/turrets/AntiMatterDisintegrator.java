package newhorizon.expand.block.turrets;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.environment.StaticWall;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import newhorizon.content.industry.IndustryLiquids;
import newhorizon.content.nuke.NukeFx;

/**
 * The Anti-Matter Disintegrator. Like the Anti-Matter Cannon it does nothing by itself: whoever controls it (a player, by the cursor, or a
 * logic processor with a shoot order for a point) holds fire to <b>charge</b> it, on power alone, and when the charge is full and the point
 * aimed at is a terrain <b>wall tile</b> in reach, it fires one beam that dissolves that one tile. Anti-matter is its ammunition: a shot
 * spends {@link #antiMatterPerShot} of it, <b>when it fires, never while it charges</b>, and cannot be made without it.
 *
 * <p>What is left is the floor under the wall, with the ore that was in it: a plain wall leaves whatever ore was already under it, and a wall
 * ore (the Erekir beryllium and tungsten, and the others) leaves the ordinary ore of the same item, which drills can mine. It never touches a
 * building, a unit or any other tile.
 */
public class AntiMatterDisintegrator extends PowerTurret{
	/** Ticks to charge from empty to full, to lose a full charge when not charging, and to cool after a shot. */
	public float chargeTicks = 120f, drainTicks = 180f, coolTicks = 60f;
	/** Anti-matter a shot spends. */
	public float antiMatterPerShot = 25f;
	/** How far the muzzle is from the middle, and how closely the barrel must be on the aim to fire. */
	public float muzzleLength = 22f, fireCone = 6f;
	public Color glow = Color.valueOf("7ff4ff"), glow2 = Color.valueOf("b45cff");

	public static final Stat ammoStat = new Stat("nh-disintegrator-ammo", StatCat.function);

	public TextureRegion baseR, outlineR;

	public enum State{
		idle, charging, ready, down, cooling
	}

	public AntiMatterDisintegrator(String name){
		super(name);
		envEnabled = Env.any;
		hasLiquids = true;
		liquidCapacity = 200f;
		targetAir = true;
		targetGround = true;
		buildType = DisintegratorBuild::new;
		//The base game's turret code only ever shoots a bullet that does nothing: the real shot is made below.
		shootType = new BasicBulletType(0f, 0f);
		reload = 1e9f;
		shootCone = 360f;
		recoil = 0f;
	}

	@Override
	public void load(){
		super.load();
		baseR = Core.atlas.find(name + "-base");
		outlineR = Core.atlas.find(name + "-outline");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(ammoStat, Core.bundle.format("stat.nh-disintegrator-ammo.value", (int)antiMatterPerShot));
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-disintegrator-tank", (DisintegratorBuild b) -> new Bar(
			() -> IndustryLiquids.antiMatter.localizedName,
			() -> IndustryLiquids.antiMatter.barColor(),
			() -> b.liquids.get(IndustryLiquids.antiMatter) / liquidCapacity));
		addBar("nh-disintegrator", (DisintegratorBuild b) -> new Bar(
			() -> Core.bundle.format("nh.disintegrator.state." + (b.state() == State.ready && !b.hasAmmoTank() ? "noammo" : b.state().name()), (int)(b.energy * 100f), (int)Math.ceil(b.cool / 60f)),
			() -> b.state() == State.cooling ? Pal.lightishOrange : b.state() == State.down ? Pal.accent : Color.valueOf("b45cff"),
			() -> b.state() == State.cooling ? 1f - b.cool / coolTicks : b.energy));
	}

	/** Whether the tile is a terrain wall that can be dissolved: a static wall with nothing built on it, and not on the outermost ring of the map. */
	public static boolean dissolvable(Tile t){
		if(t == null || Vars.world == null) return false;
		if(t.build != null) return false;
		if(t.x <= 0 || t.y <= 0 || t.x >= Vars.world.width() - 1 || t.y >= Vars.world.height() - 1) return false;
		return t.block() instanceof StaticWall;
	}

	/** The ordinary ore overlay of an item (what a wall ore of it leaves behind), or null. */
	public static Block oreOf(Item item){
		if(item == null) return null;
		for(Block b : Vars.content.blocks()){
			if(b instanceof OreBlock && !((Floor)b).wallOre && ((Floor)b).itemDrop == item) return b;
		}
		return null;
	}

	/** Takes the wall off the tile and leaves the ore underneath: a wall ore becomes the ordinary ore of its item. Server side (the tile changes are sent to everyone). */
	public static void dissolve(Tile t){
		Floor overlay = t.overlay();
		Item wallItem = overlay != null && overlay.wallOre ? overlay.itemDrop : null;
		t.removeNet();
		if(overlay != null && overlay.wallOre){
			Block ore = oreOf(wallItem);
			t.setOverlayNet(ore != null ? ore : Blocks.air);
		}
	}

	public class DisintegratorBuild extends PowerTurretBuild{
		/** How full the charge is (0 to 1), and the ticks of cooling left after a shot. */
		public float energy, cool;
		/** Whether the pilot held fire last tick, how far the barrel is kicked back, and a clock for the animations. */
		public boolean holding;
		public float kick, flare, clock;
		private boolean wasFull;

		public State state(){
			if(cool > 0f) return State.cooling;
			if(energy >= 1f) return State.ready;
			if(holding) return State.charging;
			return energy > 0f ? State.down : State.idle;
		}

		public boolean hasAmmoTank(){
			return liquids.get(IndustryLiquids.antiMatter) >= antiMatterPerShot - 0.0001f;
		}

		/** The tile aimed at, if it can be dissolved and is in reach. */
		public Tile aimed(){
			Tile t = Vars.world.tileWorld(targetPos.x, targetPos.y);
			if(!dissolvable(t) || !within(t.worldx(), t.worldy(), AntiMatterDisintegrator.this.range)) return null;
			return t;
		}

		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return liquid == IndustryLiquids.antiMatter && liquids.get(liquid) < liquidCapacity - 0.001f;
		}

		/** No target of its own, ever. */
		@Override
		protected void findTarget(){
			target = null;
		}

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
			kick = Math.max(0f, kick - Time.delta / 30f);
			flare = Math.max(0f, flare - Time.delta / 20f);
			boolean pilot = isControlled() || logicControlled();
			holding = (isControlled() && unit != null && unit.isShooting()) || (!isControlled() && logicControlled() && logicShooting);

			if(cool > 0f){
				cool = Math.max(0f, cool - Time.delta);
				energy = 0f;
			}else if(holding && efficiency > 0f){
				energy = Math.min(1f, energy + Time.delta * efficiency / chargeTicks);
			}else{
				energy = Math.max(0f, energy - Time.delta / drainTicks);
			}
			if(!wasFull && energy >= 1f){
				if(!Vars.headless) Sounds.chargeLancer.at(x, y, 0.8f, 1.2f);
				flare = 0.6f;
			}
			wasFull = energy >= 1f;

			//The charge costs no anti-matter, but a shot needs a shot's worth in the tank, and a wall under the aim.
			if(Vars.net.client() || !pilot || !holding || energy < 1f || cool > 0f || !hasAmmoTank()) return;
			Tile t = aimed();
			if(t == null) return;
			if(Angles.angleDist(rotation, angleTo(t.worldx(), t.worldy())) > fireCone) return;
			fire(t);
		}

		/** Spends the charge and the anti-matter on one beam at the tile, and takes the wall off it. */
		public void fire(Tile t){
			float mx = x + Angles.trnsx(rotation, muzzleLength), my = y + Angles.trnsy(rotation, muzzleLength);
			float tx = t.worldx(), ty = t.worldy(), len = Mathf.dst(mx, my, tx, ty), angle = Angles.angle(mx, my, tx, ty);
			Call.effect(NukeFx.antiBeam, mx, my, angle, new Color(Math.min(1f, len / 1020f), 0f, 0f, 1f), Float.valueOf(len));
			Call.effect(NukeFx.antiPop, tx, ty, 10f, Color.white);
			dissolve(t);
			//The anti-matter is spent now, as the beam goes out.
			liquids.remove(IndustryLiquids.antiMatter, antiMatterPerShot);
			energy = 0f;
			cool = coolTicks;
			kick = 1f;
			flare = 1f;
			wasFull = false;
			Effect_shake();
			Sounds.explosion.at(x, y, 0.8f, 1.4f);
		}

		private void Effect_shake(){
			mindustry.entities.Effect.shake(5f, 20f, this);
		}

		@Override
		public void draw(){
			Draw.rect(baseR, x, y);
			Draw.z(Layer.turret);
			float ox = x - Angles.trnsx(rotation, 5f * kick), oy = y - Angles.trnsy(rotation, 5f * kick);
			Drawf.shadow(region, ox - elevation, oy - elevation, rotation - 90f);
			if(outlineR != null && outlineR.found()) Draw.rect(outlineR, ox, oy, rotation - 90f);
			Draw.rect(region, ox, oy, rotation - 90f);
			float lvl = energy;
			Draw.z(Layer.turret + 0.5f);
			Draw.blend(Blending.additive);
			if(lvl > 0.02f || flare > 0f){
				float pulse = state() == State.ready ? 1f + 0.12f * Mathf.absin(clock, 5f, 1f) : 1f;
				float r = (1.5f + 6f * lvl) * pulse + 9f * flare;
				float cx = ox + Angles.trnsx(rotation, muzzleLength - 2f), cy = oy + Angles.trnsy(rotation, muzzleLength - 2f);
				Draw.color(glow2, 0.35f + 0.4f * lvl);
				Fill.circle(cx, cy, r * 1.7f);
				Draw.color(glow, 0.6f + 0.4f * lvl);
				Fill.circle(cx, cy, r);
				Draw.color(Color.white, 1f);
				Fill.circle(cx, cy, r * 0.5f);
				Drawf.light(cx, cy, r * 8f, glow, 0.5f * Math.max(lvl, flare));
			}
			if(lvl > 0.02f){
				Draw.color(glow, 0.3f + 0.7f * lvl);
				Draw.rect(Core.atlas.white(), ox + Angles.trnsx(rotation, 2f), oy + Angles.trnsy(rotation, 2f), 3f, 10f * Math.min(1f, 0.4f + lvl), rotation);
			}
			Draw.blend();
			Draw.color();
			Draw.reset();
		}

		/** What a processor can read besides the usual: how full the charge is, and how much of the cooldown is left. */
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
