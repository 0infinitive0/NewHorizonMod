package newhorizon.expand.units.nuke;

import arc.Events;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.storage.CoreBlock;
import newhorizon.content.nuke.NukeFx;
import newhorizon.expand.block.defence.ShieldGenerator;

import static mindustry.Vars.tilesize;

/**
 * What the anti-matter cannon's beam does. {@link #pick} chooses what is under the cursor, {@link #shield} finds a shield in the way of the
 * beam, and {@link #annihilate} destroys the target and starts a ripple: every {@link #stepTicks} ticks it destroys everything of the
 * target's team that touches what was destroyed in the step before, until nothing new is touched or {@link #cap} things have gone. A core is
 * never destroyed by the ripple and does not carry it on. Everything here is done on the server.
 */
public class AntiMatter{
	/** How near the cursor a target has to be (besides its own size), ticks between two steps of a ripple, and the most a ripple destroys. */
	public static float snap = 24f;
	public static float stepTicks = 6f;
	public static int cap = 4000;

	/** A ripple that is going. */
	private static class Wave{
		Team team;
		final Seq<Building> buildings = new Seq<>();
		final Seq<Unit> units = new Seq<>();
		final IntSet seenBuildings = new IntSet(), seenUnits = new IntSet();
		float timer;
		int destroyed;
	}

	private static final Seq<Wave> waves = new Seq<>();
	private static boolean hooked;

	public static void hook(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, AntiMatter::update);
		Events.on(EventType.ResetEvent.class, e -> waves.clear());
		Events.on(EventType.WorldLoadEvent.class, e -> waves.clear());
	}

	/** How many ripples are going now. */
	public static int active(){
		return waves.size;
	}

	/** The enemy structure or unit nearest the cursor point, near enough to it and within range of the muzzle; null if there is none. */
	public static Position pick(Team team, float aimX, float aimY, float fromX, float fromY, float range){
		Object[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		float scan = snap + 60f;
		Units.nearby(aimX - scan, aimY - scan, scan * 2f, scan * 2f, u -> {
			if(u.team == team || u.dead() || !u.isAdded() || !u.targetable(team) || !u.type.killable(u)) return;
			float d = u.dst(aimX, aimY);
			if(d > snap + u.hitSize / 2f || u.dst(fromX, fromY) > range + u.hitSize / 2f || d >= bestDst[0]) return;
			bestDst[0] = d;
			best[0] = u;
		});
		Units.nearbyBuildings(aimX, aimY, scan, b -> {
			if(b.team == team || !b.isValid()) return;
			float d = b.dst(aimX, aimY);
			if(d > snap + b.hitSize() / 2f || b.dst(fromX, fromY) > range + b.hitSize() / 2f || d >= bestDst[0]) return;
			bestDst[0] = d;
			best[0] = b;
		});
		return (Position)best[0];
	}

	/** A shield that stops the beam: the structure that makes it, and where the beam enters it. */
	public static class Block{
		public Building building;
		public float x, y;
	}

	/** The first working shield of another team that the line from the muzzle to the target crosses before the target; null if the way is clear. */
	public static Block shield(Team shooter, float x1, float y1, float x2, float y2, Building ignore){
		Block best = null;
		float bestT = Float.MAX_VALUE;
		float dx = x2 - x1, dy = y2 - y1, len = Mathf.dst(dx, dy);
		if(len < 1f) return null;
		for(Teams.TeamData data : Vars.state.teams.present){
			if(data.team == shooter) continue;
			for(Building b : data.buildings){
				if(b == ignore || !b.isValid()) continue;
				float t = -1f;
				if(b instanceof ForceProjector.ForceBuild){
					ForceProjector.ForceBuild f = (ForceProjector.ForceBuild)b;
					if(f.broken || f.radscl <= 0.05f) continue;
					t = enter(x1, y1, dx, dy, f.x, f.y, f.realRadius());
				}else if(b instanceof BaseShield.BaseShieldBuild){
					BaseShield.BaseShieldBuild s = (BaseShield.BaseShieldBuild)b;
					if(s.broken || s.radius() <= 4f || !s.isValid()) continue;
					t = enter(x1, y1, dx, dy, s.x, s.y, s.radius());
				}else if(b instanceof ShieldGenerator.ShieldGeneratorBuild){
					ShieldGenerator.ShieldGeneratorBuild g = (ShieldGenerator.ShieldGeneratorBuild)b;
					if(g.broken || g.radscl <= 0f || g.backCenter == null) continue;
					t = enterArc(x1, y1, dx, dy, g.backCenter.x, g.backCenter.y, 250f, g.rotation, ((ShieldGenerator)g.block).shieldArc);
				}
				if(t < 0f || t > 1f || t >= bestT) continue;
				bestT = t;
				best = new Block();
				best.building = b;
				best.x = x1 + dx * t;
				best.y = y1 + dy * t;
			}
		}
		return best;
	}

	/** Where along the line (0 at the start, 1 at the end) it first goes into the circle, or the start if it begins inside; -1 if it never does. */
	static float enter(float x, float y, float dx, float dy, float cx, float cy, float r){
		float fx = x - cx, fy = y - cy;
		float a = dx * dx + dy * dy, b = 2f * (fx * dx + fy * dy), c = fx * fx + fy * fy - r * r;
		if(c <= 0f) return 0f;
		float disc = b * b - 4f * a * c;
		if(disc < 0f) return -1f;
		float t = (-b - (float)Math.sqrt(disc)) / (2f * a);
		return t >= 0f && t <= 1f ? t : -1f;
	}

	/** The same for a shield that is only part of a circle: the arc that is {@code arc} degrees wide round the direction {@code facing}. */
	static float enterArc(float x, float y, float dx, float dy, float cx, float cy, float r, float facing, float arc){
		float fx = x - cx, fy = y - cy;
		float a = dx * dx + dy * dy, b = 2f * (fx * dx + fy * dy), c = fx * fx + fy * fy - r * r;
		float disc = b * b - 4f * a * c;
		if(disc < 0f) return -1f;
		float root = (float)Math.sqrt(disc), best = -1f;
		for(float t : new float[]{(-b - root) / (2f * a), (-b + root) / (2f * a)}){
			if(t < 0f || t > 1f) continue;
			float px = x + dx * t - cx, py = y + dy * t - cy;
			if(Angles.within(facing, Mathf.atan2(px, py) * Mathf.radDeg, arc / 2f) && (best < 0f || t < best)) best = t;
		}
		return best;
	}

	/** The beam stopped at a shield: only the structure that makes the shield is destroyed, and nothing spreads. */
	public static void destroyShield(Block hit){
		if(Vars.net.client() || hit == null || !hit.building.isValid()) return;
		destroy(hit.building);
	}

	/** Destroys one structure or unit, with an effect, if it can be destroyed. */
	private static boolean destroy(Position p){
		if(p instanceof Unit){
			Unit u = (Unit)p;
			if(u.dead() || !u.isAdded() || !u.type.killable(u)) return false;
			NukeFx.antiPop.at(u.x, u.y, Math.max(12f, u.hitSize), arc.graphics.Color.white);
			u.kill();
			return true;
		}
		if(p instanceof Building){
			Building b = (Building)p;
			if(!b.isValid()) return false;
			NukeFx.antiPop.at(b.x, b.y, Math.max(12f, b.block.size * tilesize), arc.graphics.Color.white);
			b.kill();
			return true;
		}
		return false;
	}

	/**
	 * Destroys the target outright and starts the ripple from it (the target's team's structures and units that touch it come next). Server
	 * side. Returns whether the target was destroyed.
	 */
	public static boolean annihilate(Position target){
		if(Vars.net.client()) return false;
		hook();
		Team team = target instanceof Unit ? ((Unit)target).team : target instanceof Building ? ((Building)target).team : null;
		if(team == null) return false;
		Wave wave = new Wave();
		wave.team = team;
		if(target instanceof Building) wave.seenBuildings.add(((Building)target).pos());
		if(target instanceof Unit) wave.seenUnits.add(((Unit)target).id);
		//What touches it is found before it is gone.
		touching(wave, target);
		boolean ok = destroy(target);
		if(!ok) return false;
		wave.destroyed = 1;
		wave.timer = stepTicks;
		if(!wave.buildings.isEmpty() || !wave.units.isEmpty()) waves.add(wave);
		return true;
	}

	/** Adds to the wave what touches the thing: the structures and units of its team it has not reached yet. A core is skipped. */
	private static void touching(Wave wave, Position p){
		if(p instanceof Building){
			Building b = (Building)p;
			Seq<Tile> tiles = new Seq<>();
			b.tile.getLinkedTilesAs(b.block, tiles);
			for(Tile t : tiles){
				for(Point2Edge e : EDGES){
					Tile n = Vars.world.tile(t.x + e.x, t.y + e.y);
					if(n == null || n.build == null) continue;
					Building nb = n.build;
					if(nb == b || nb.team != wave.team || !nb.isValid() || nb instanceof CoreBlock.CoreBuild) continue;
					if(wave.seenBuildings.add(nb.pos())) wave.buildings.add(nb);
				}
			}
			float half = b.block.size * tilesize / 2f;
			float scan = half + 80f;
			Units.nearby(b.x - scan, b.y - scan, scan * 2f, scan * 2f, u -> {
				if(u.team != wave.team || u.dead() || !u.isAdded()) return;
				float reach = half + u.hitSize / 2f + 2f;
				if(Math.abs(u.x - b.x) <= reach && Math.abs(u.y - b.y) <= reach && wave.seenUnits.add(u.id)) wave.units.add(u);
			});
		}else if(p instanceof Unit){
			Unit u = (Unit)p;
			float scan = u.hitSize / 2f + 100f;
			Units.nearby(u.x - scan, u.y - scan, scan * 2f, scan * 2f, o -> {
				if(o == u || o.team != wave.team || o.dead() || !o.isAdded()) return;
				if(o.within(u, (o.hitSize + u.hitSize) / 2f + 3f) && wave.seenUnits.add(o.id)) wave.units.add(o);
			});
			Units.nearbyBuildings(u.x, u.y, u.hitSize / 2f + 4f, nb -> {
				if(nb.team != wave.team || !nb.isValid() || nb instanceof CoreBlock.CoreBuild) return;
				float reach = nb.block.size * tilesize / 2f + u.hitSize / 2f + 2f;
				if(Math.abs(u.x - nb.x) <= reach && Math.abs(u.y - nb.y) <= reach && wave.seenBuildings.add(nb.pos())) wave.buildings.add(nb);
			});
		}
	}

	private static class Point2Edge{
		final int x, y;
		Point2Edge(int x, int y){
			this.x = x;
			this.y = y;
		}
	}

	private static final Point2Edge[] EDGES;

	static{
		EDGES = new Point2Edge[Geometry.d4.length];
		for(int i = 0; i < EDGES.length; i++) EDGES[i] = new Point2Edge(Geometry.d4[i].x, Geometry.d4[i].y);
	}

	/** One tick: every ripple that is due takes a step. */
	private static void update(){
		if(Vars.net.client() || waves.isEmpty()) return;
		for(int i = waves.size - 1; i >= 0; i--){
			Wave w = waves.get(i);
			w.timer -= Time.delta;
			if(w.timer > 0f) continue;
			w.timer += stepTicks;
			step(w);
			if(w.buildings.isEmpty() && w.units.isEmpty()) waves.remove(i);
		}
	}

	/** The things waiting are destroyed, and what touches them waits for the next step. */
	private static void step(Wave w){
		Seq<Building> bs = new Seq<>(w.buildings);
		Seq<Unit> us = new Seq<>(w.units);
		w.buildings.clear();
		w.units.clear();
		for(Building b : bs){
			if(w.destroyed >= cap) break;
			if(!b.isValid()) continue;
			touching(w, b);
			if(destroy(b)) w.destroyed++;
		}
		for(Unit u : us){
			if(w.destroyed >= cap) break;
			if(u.dead() || !u.isAdded()) continue;
			touching(w, u);
			if(destroy(u)) w.destroyed++;
		}
		if(w.destroyed >= cap){
			w.buildings.clear();
			w.units.clear();
		}
	}

	/** Stops every ripple (for tests and for a new map). */
	public static void clear(){
		waves.clear();
	}
}
