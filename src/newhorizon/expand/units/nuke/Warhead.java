package newhorizon.expand.units.nuke;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Fires;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Payloadc;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import newhorizon.content.nuke.NukeBlocks;
import newhorizon.content.nuke.NukeBullets;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeStatus;
import newhorizon.expand.block.chronos.TemporalDisable;
import newhorizon.expand.bullets.nuke.ZoneBulletType;

import static mindustry.Vars.tilesize;

/**
 * One kind of nuke. An inactive nuke of any kind makes the same plain blast; an active one makes its kind's blast, see {@link #detonate}.
 * Most kinds do damage to everything in reach, the nuke's own side included, and then something of their own ({@link #special}). The
 * kinds that change things (psychic, spatial, temporal, infantry) do no damage at all: it would only kill what they act on.
 */
public abstract class Warhead{
	/** The blast of an inactive nuke: the same for every kind, with no special effect. */
	public static final float inactiveRadius = 70f, inactiveDamage = 340f;

	public static Warhead
		explosive, incendiary, radiation, emp,
		psychic, gravity, spatial, shockwave, cluster, wall, temporal, infantry, overload;
	public static final Seq<Warhead> all = new Seq<>();

	public final String name;
	public final Color color;
	/** Radius and damage of the active blast. Zero damage means the blast hurts nothing. */
	public final float radius, damage;

	protected Warhead(String name, Color color, float radius, float damage){
		this.name = name;
		this.color = color;
		this.radius = radius;
		this.damage = damage;
		all.add(this);
	}

	/** What the active blast looks like. Runs on every side. */
	public void visual(float x, float y){
		NukeFx.blast.at(x, y, radius, color);
	}

	/** The whole active blast, server side: damage to everything in reach, then the kind's own effect. {@code nuke} is the dead unit. */
	public void detonate(float x, float y, Team team, Unit nuke){
		if(damage > 0f) Damage.damage(Team.derelict, x, y, radius, damage, true, true, true);
		special(x, y, team, nuke);
	}

	/** What the blast does beyond its damage. {@code team} is the nuke's own. Server side only. */
	public abstract void special(float x, float y, Team team, Unit nuke);

	public static void load(){
		all.clear();

		explosive = new Warhead("explosive", Color.valueOf("ffb347"), 230f, 2800f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				//Pure force: the second, smaller blast is the shockwave that follows the first.
				Damage.damage(Team.derelict, x, y, radius * 0.45f, damage * 0.5f, true, true, true);
			}
		};

		incendiary = new Warhead("incendiary", Color.valueOf("ff6a3d"), 190f, 900f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Damage.createIncend(x, y, radius * 0.9f, 60);
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u.within(x, y, radius + u.hitSize / 2f)) u.apply(StatusEffects.burning, 60f * 8f);
				});
				burnGround(x, y, radius * 0.85f);
				zone(NukeBullets.incendiaryZone, x, y);
			}
		};

		radiation = new Warhead("radiation", Color.valueOf("9bff4a"), 170f, 650f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				zone(NukeBullets.radiationZone, x, y);
			}
		};

		emp = new Warhead("emp", Color.valueOf("7fd8ff"), 260f, 260f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				float r2 = radius * radius;
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u.within(x, y, radius + u.hitSize / 2f)) u.apply(NukeStatus.empShock, 60f * 15f);
				});
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.dst2(x, y) > r2 || b.power == null) return;
					//Whatever is stored is gone, and whatever runs on power stops for a while.
					if(b.power.graph != null) b.power.graph.useBatteries(b.power.graph.getBatteryStored());
					TemporalDisable.disable(b, 60f * 15f, true);
				});
				NukeFx.empPulse.at(x, y, radius, color);
			}
		};

		//Every enemy unit in reach changes sides. Nothing is damaged.
		psychic = new Warhead("psychic", Color.valueOf("e08bff"), 230f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Seq<Unit> victims = new Seq<>();
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u.within(x, y, radius + u.hitSize / 2f) && convertible(u, team)) victims.add(u);
				});
				for(Unit u : victims) convert(u, team);
			}
		};

		//Pulls everything in for a few seconds, and only then explodes.
		gravity = new Warhead("gravity", Color.valueOf("9b7bff"), 190f, 1900f){
			@Override
			public void visual(float x, float y){
				NukeFx.pullRing.at(x, y, radius * 1.3f, color);
			}

			@Override
			public void detonate(float x, float y, Team team, Unit nuke){
				NukeBullets.gravityZone.create(null, Team.derelict, x, y, 0f);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}

			/** The blast itself, when the pull is over. */
			public void finish(float x, float y){
				NukeBlast.blastAt(x, y, radius, damage, color, true);
			}
		};

		//Every unit in reach is sent to a random place on the map.
		spatial = new Warhead("spatial", Color.valueOf("3fd8a8"), 220f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Seq<Unit> units = new Seq<>();
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(!u.dead() && u != nuke && u.within(x, y, radius + u.hitSize / 2f)) units.add(u);
				});
				for(Unit u : units) scatter(u, color);
			}
		};

		//The opposite of gravity: everything in reach is thrown away from the centre.
		shockwave = new Warhead("shockwave", Color.valueOf("c8e8ff"), 280f, 700f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				NukeBullets.shockZone.create(null, Team.derelict, x, y, 0f);
			}
		};

		//Fills every free tile with a mine.
		cluster = new Warhead("cluster", Color.valueOf("d6a21f"), 200f, 350f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				NukeBlast.fill(x, y, radius, NukeBlocks.mine, team);
			}
		};

		//Fills every free tile with the wall type the nuke was set to.
		wall = new Warhead("wall", Color.valueOf("aab2c0"), 170f, 300f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				NukeAbility ability = NukeAI.abilityOf(nuke);
				NukeBlast.fill(x, y, radius, ability == null ? NukeAbility.defaultWall() : ability.wallType(), team);
			}
		};

		//Freezes and disarms every unit and turret in reach for three minutes.
		temporal = new Warhead("temporal", Color.valueOf("2b9bff"), 260f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				float r2 = radius * radius;
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u != nuke && u.within(x, y, radius + u.hitSize / 2f)) u.apply(NukeStatus.temporalLock, lockTime);
				});
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.dst2(x, y) <= r2 && b.block instanceof Turret) TemporalDisable.disable(b, lockTime, false);
				});
			}
		};

		//Every unit in reach explodes, whichever side it is on, in a blast of its own that does five times its health in damage.
		overload = new Warhead("overload", Color.valueOf("ff4d6d"), 240f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				//What each unit is, as it is when the nuke goes off: the blasts that follow change all of it.
				Seq<Unit> units = new Seq<>();
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u != nuke && !u.dead() && u.within(x, y, radius + u.hitSize / 2f)) units.add(u);
				});
				float[][] spec = new float[units.size][];
				for(int i = 0; i < units.size; i++){
					Unit u = units.get(i);
					spec[i] = new float[]{u.x, u.y, overloadRadius(u), u.health * 5f};
				}

				for(int i = 0; i < units.size; i++){
					Unit u = units.get(i);
					if(!u.dead()) u.kill();
					float[] s = spec[i];
					NukeFx.blast.at(s[0], s[1], s[2], color);
					if(s[3] > 0f) Damage.damage(Team.derelict, s[0], s[1], s[2], s[3], true, true, true);
				}
			}
		};

		//Carries units, which come out where it goes off. It hurts nothing.
		infantry = new Warhead("infantry", Color.valueOf("57b36b"), 120f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				if(!(nuke instanceof Payloadc)) return;
				Payloadc carrier = (Payloadc)nuke;
				Seq<Payload> cargo = new Seq<>(carrier.payloads());
				carrier.payloads().clear();
				for(Payload payload : cargo){
					if(!(payload instanceof UnitPayload)) continue;
					Unit u = ((UnitPayload)payload).unit;
					Tmp.v1.trns(Mathf.random(360f), Mathf.random(radius * 0.5f));
					u.set(x + Tmp.v1.x, y + Tmp.v1.y);
					u.vel.setZero();
					u.add();
				}
			}
		};
	}

	/** How long the temporal nuke's freeze lasts: three minutes. */
	public static final float lockTime = 60f * 60f * 3f;

	/** Called by the zone of a nuke that waits before it explodes. */
	public void finish(float x, float y){
	}

	// ------------------------------------------------------------------ helpers shared by the kinds

	/** Leaves a lingering patch of this kind. */
	protected static void zone(ZoneBulletType type, float x, float y){
		type.create(null, Team.derelict, x, y, 0f);
	}

	/** Sets fires on the ground, a few at random places within the radius. */
	protected static void burnGround(float x, float y, float range){
		int tr = (int)(range / tilesize);
		for(int i = 0; i < 40; i++){
			float a = Mathf.random(360f), d = Mathf.sqrt(Mathf.random()) * tr;
			Tile tile = Vars.world.tileWorld(x + Mathf.cosDeg(a) * d * tilesize, y + Mathf.sinDeg(a) * d * tilesize);
			if(tile != null) Fires.create(tile);
		}
	}

	/** Whether the unit would change sides: an enemy of the nuke's team that nobody is flying. */
	public static boolean convertible(Unit u, Team to){
		return !u.dead() && u.team != to && u.team != Team.derelict && !u.isPlayer();
	}

	/** Puts the unit on the given team, under that team's control. */
	public static void convert(Unit u, Team to){
		u.team(to);
		u.controller(u.type.createController(u));
		NukeFx.armed.at(u.x, u.y);
		NukeFx.empPulse.at(u.x, u.y, u.hitSize * 2f, to.color);
	}

	/** Sends the unit to a random place on the map. Anywhere will do: a walking unit can land in the water. */
	public static boolean scatter(Unit u, Color color){
		int tx = Mathf.random(1, Vars.world.width() - 2), ty = Mathf.random(1, Vars.world.height() - 2);
		Tile tile = Vars.world.tile(tx, ty);
		if(tile == null) return false;

		NukeFx.empPulse.at(u.x, u.y, u.hitSize * 3f, color);
		u.set(tile.worldx(), tile.worldy());
		u.vel.setZero();
		NukeFx.empPulse.at(u.x, u.y, u.hitSize * 3f, color);
		return true;
	}

	/** The radius of the blast a unit of this size makes in an overload nuke: always much smaller than the nuke's. */
	public static float overloadRadius(Unit u){
		return Mathf.clamp(u.hitSize * 2.2f + 20f, 36f, 100f);
	}
}
