package newhorizon.expand.units.nuke;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Fires;
import mindustry.entities.Puddles;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Payloadc;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import newhorizon.content.nuke.NukeBlocks;
import newhorizon.content.nuke.NukeBullets;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeStatus;
import newhorizon.content.nuke.NukeUnits;
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
		psychic, gravity, spatial, shockwave, cluster, wall, temporal, infantry, overload,
		laser, swarm, storm, decoy, mini, lightning, siphon, quake,
		scatter, magnet, hijack, scramble, sabotage, breach, devolve, meteor, neoplasm;
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

	/**
	 * Lingering things the blast leaves (patches, pulls, beams, missiles): bullets, which every side simulates for itself, so this runs on
	 * every side, unlike {@link #detonate}. {@code nuke} is the dead unit.
	 */
	public void spawn(float x, float y, Team team, Unit nuke){
	}

	/** The whole active blast, server side: damage to everything in reach, then the kind's own effect. {@code nuke} is the dead unit. */
	public void detonate(float x, float y, Team team, Unit nuke){
		if(damage > 0f) Damage.damage(Team.derelict, x, y, radius, damage, true, true, true);
		special(x, y, team, nuke);
	}

	/**
	 * What an inactive nuke of this kind does when it is destroyed, on top of the plain inactive blast. Nothing for most kinds. Server side.
	 */
	public void inactiveSpecial(float x, float y, Team team, Unit nuke){
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
				burnBuildings(x, y, radius * 0.9f);
				splash(Liquids.oil, 26, x, y, radius * 0.9f);
				splash(Liquids.slag, 16, x, y, radius * 0.9f);
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				zone(NukeBullets.incendiaryZone, x, y);
			}
		};

		//Its damage is a share of what each thing has, not a fixed amount: a first dose when it goes off, then the fallout (3% a second).
		radiation = new Warhead("radiation", Color.valueOf("9bff4a"), 170f, 650f){
			@Override
			public void detonate(float x, float y, Team team, Unit nuke){
				//No flat blast damage: only the dose in special.
				special(x, y, team, nuke);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(!u.dead() && u != nuke && u.within(x, y, radius + u.hitSize / 2f)) u.damagePierce(u.maxHealth * firstDose);
				});
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.dst(x, y) <= radius) b.damagePierce(b.maxHealth * firstDose);
				});
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				zone(NukeBullets.radiationZone, x, y);
			}
		};

		//The pulse has no blast. Every flying unit in reach dies on the spot; every other unit is stopped and disarmed for fifteen seconds.
		//Every battery, power generator and power node (and diode and beam node) in reach is destroyed outright, and whatever else runs on
		//power loses what it had and is switched off for fifteen seconds. Nothing is damaged by the pulse itself.
		emp = new Warhead("emp", Color.valueOf("7fd8ff"), 260f, 0f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				float r2 = radius * radius;
				Seq<Unit> flying = new Seq<>();
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u == nuke || u.dead() || !u.within(x, y, radius + u.hitSize / 2f)) return;
					//A nuke that was launched from orbit is too high for it.
					if(u.isFlying() && !u.hasEffect(NukeStatus.highAltitude)) flying.add(u);
					else u.apply(NukeStatus.empShock, 60f * 15f);
				});
				for(Unit u : flying) u.kill();

				Seq<Building> powered = new Seq<>();
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.isValid() && b.dst2(x, y) <= r2 && (b.power != null || fried(b.block))) powered.add(b);
				});
				for(Building b : powered){
					if(fried(b.block)){
						//Emptied first, so that a charged battery does not blow up with what it held.
						if(b.power != null) b.power.status = 0f;
						b.kill();
					}else{
						b.power.status = 0f;
						TemporalDisable.disable(b, 60f * 15f, true);
					}
				}
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
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeBullets.gravityZone.create(null, Team.derelict, x, y, 0f);
			}

			@Override
			public void detonate(float x, float y, Team team, Unit nuke){
				//Nothing yet: the blast comes when the pull is over.
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
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
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

		//Fills every free tile with the wall type the nuke was set to. The walls belong to no one (the derelict team), so they hold for
		//everybody alike: nobody's turrets and units take them for an enemy or an ally, and anyone can break them or take them over.
		wall = new Warhead("wall", Color.valueOf("aab2c0"), 170f, 300f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				NukeAbility ability = NukeAI.abilityOf(nuke);
				NukeBlast.fill(x, y, radius, ability == null ? NukeAbility.defaultWall() : ability.wallType(), Team.derelict);
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

		//Four meltdown lasers from the centre, turning slowly through a full circle. They burn everything in their way, whichever side.
		laser = new Warhead("laser", Color.valueOf("f2f2f2"), 150f, 300f){
			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				float start = nuke == null ? Mathf.random(360f) : (nuke.id * 97f) % 360f;
				for(int i = 0; i < beams; i++){
					NukeBullets.spinLaser.create(null, Team.derelict, x, y, start + i * 360f / beams);
				}
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//Three waves of homing missiles, a second apart, each wave a ring of them. They belong to the nuke's team and home in on its enemies:
		//homing only looks for enemies, which a neutral shot does not have.
		swarm = new Warhead("swarm", Color.valueOf("ffd84d"), 130f, 200f){
			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeBullets.swarmSpawner.create(null, team, x, y, 0f);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//A tornado that drifts about for ten seconds: it drags units in, swings them round, carries them a short way and throws them outward,
		//burning everything in reach and throwing the odd bolt of lightning. It hurts and moves everything, whichever side.
		storm = new Warhead("storm", Color.valueOf("9bb7d4"), 170f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.pullRing.at(x, y, radius, color);
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				//Which way it drifts comes from the nuke, so that every side makes the same tornado.
				float heading = nuke == null ? Mathf.random(360f) : (nuke.id * 137.5f) % 360f;
				NukeBullets.tornado.create(null, Team.derelict, x, y, heading);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//A ball of light that hangs where the nuke went off and strikes arcs of lightning around it for ten seconds. The blast hurts nothing.
		lightning = new Warhead("lightning", Color.valueOf("ffe066"), 210f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeBullets.arcOrb.create(null, Team.derelict, x, y, 0f);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//A purple orb that strikes one arc at a time, again and again, and lives as long as there is something to strike. Left alone for
		//half a minute it begins to drag in every unit around it. The blast hurts nothing; point defence can shoot the orb down.
		siphon = new Warhead("siphon", Color.valueOf("9c3fe8"), 210f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeBullets.siphonOrb.create(null, Team.derelict, x, y, 0f);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//An earthquake: it tears at structures and hardly touches units. The blast itself hurts nothing.
		quake = new Warhead("quake", Color.valueOf("b08968"), 290f, 0f){
			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeBullets.quake.create(null, Team.derelict, x, y, 0f);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//Throws every structure in reach to a random free place around. Turrets, containers, tanks and batteries keep what they hold.
		scatter = new Warhead("scatter", Color.valueOf("ff9ec4"), 230f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				BlockMover.scatter(x, y, radius, radius * 1.6f);
			}
		};

		//Draws every structure in reach in towards the centre and packs them together. The same things keep what they hold.
		magnet = new Warhead("magnet", Color.valueOf("5c8dff"), 260f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.pullRing.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				BlockMover.magnet(x, y, radius);
			}
		};

		//Every structure of another team in reach changes sides, turrets included. Cores stay. Nothing is damaged.
		hijack = new Warhead("hijack", Color.valueOf("ffd1a0"), 240f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Seq<Building> taken = new Seq<>();
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.dst(x, y) <= radius && b.team != team && BlockMover.movable(b)) taken.add(b);
				});
				for(Building b : taken){
					b.changeTeam(team);
					NukeFx.armed.at(b.x, b.y);
				}
			}
		};

		//Turns every rotating structure in reach a random way and wipes or randomises the settings of the rest: sorters and unloaders pick
		//a random item, bridges and nodes unlink, factories forget what they were making. Nothing is damaged.
		scramble = new Warhead("scramble", Color.valueOf("ff7ad9"), 240f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				for(Building b : BlockMover.within(x, y, radius)){
					BlockMover.enqueue(() -> scrambleOne(b));
				}
			}
		};

		//Empties every turret in reach of its ammo and sets off what is stored: ammo and anything flammable or explosive blows up in
		//proportion to how dangerous it is, which can set off the next structure along.
		sabotage = new Warhead("sabotage", Color.valueOf("e8590c"), 230f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.blast.at(x, y, radius * 0.6f, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.dst(x, y) > radius || !b.isValid()) return;
					sabotageOne(b);
				});
			}
		};

		//A cut along the way the nuke came: everything in a narrow lane behind the point where it went off takes enormous damage, armor
		//or not, which opens a way through layered walls towards the target.
		breach = new Warhead("breach", Color.valueOf("fff1c9"), 120f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.blast.at(x, y, radius, color);
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeFx.breachBeam.at(x, y, heading(nuke), color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				float angle = heading(nuke);
				float ax = x + Mathf.cosDeg(angle + 180f) * laneBack, ay = y + Mathf.sinDeg(angle + 180f) * laneBack;
				float bx = x + Mathf.cosDeg(angle) * laneFront, by = y + Mathf.sinDeg(angle) * laneFront;
				float mx = (ax + bx) / 2f, my = (ay + by) / 2f, reach = (laneBack + laneFront) / 2f + 60f;
				Units.nearbyBuildings(mx, my, reach, b -> {
					if(b.isValid() && Intersector.distanceSegmentPoint(ax, ay, bx, by, b.x, b.y) <= laneWidth / 2f + b.block.size * tilesize / 2f) b.damagePierce(laneDamage);
				});
				Units.nearby(mx - reach, my - reach, reach * 2f, reach * 2f, u -> {
					if(u != nuke && !u.dead() && Intersector.distanceSegmentPoint(ax, ay, bx, by, u.x, u.y) <= laneWidth / 2f + u.hitSize / 2f) u.damagePierce(laneUnitDamage);
				});
			}
		};

		//Every unit in reach drops a tier; walls fall back to copper walls, conveyors to basic ones, turrets to duos, menders and batteries to
		//their one-tile versions and power generators to combustion generators. Nothing is damaged.
		devolve = new Warhead("devolve", Color.valueOf("b89b72"), 250f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				Seq<Unit> units = new Seq<>();
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u != nuke && u.within(x, y, radius + u.hitSize / 2f)) units.add(u);
				});
				for(Unit u : units){
					if(Devolve.unit(u)) NukeFx.armed.at(u.x, u.y);
				}
				for(Building b : BlockMover.within(x, y, radius)){
					if(Devolve.target(b.block) != null) BlockMover.enqueue(() -> Devolve.block(b));
				}
			}
		};

		//Ten seconds of meteors, one every fifth of a second, each landing on a random spot in reach with a shadow and a warning ring first
		//and a blast of its own. The nuke's own blast hurts nothing. Everything on the ground and every structure is hit, of every team.
		meteor = new Warhead("meteor", Color.valueOf("d9822b"), 230f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void spawn(float x, float y, Team team, Unit nuke){
				NukeBullets.meteorStorm.create(null, Team.derelict, x, y, 0f);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
			}
		};

		//Covers every tile in reach with neoplasm, the red liquid of the base game: it settles in puddles that keep creeping through
		//blocks and over the ground for a while. Nothing is damaged: the neoplasm of the base game has no effect of its own.
		neoplasm = new Warhead("neoplasm", Color.valueOf("c33e2b"), 170f, 0f){
			@Override
			public void visual(float x, float y){
				NukeFx.empPulse.at(x, y, radius, color);
			}

			@Override
			public void special(float x, float y, Team team, Unit nuke){
				cover(Liquids.neoplasm, x, y, radius);
			}
		};

		//A tough bait that breaks into three mini nukes, which fly off at whatever is near. The blast itself hurts nothing.
		decoy = new Warhead("decoy", Color.valueOf("c3ccd8"), 60f, 0f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
				split(x, y, team);
			}

			@Override
			public void inactiveSpecial(float x, float y, Team team, Unit nuke){
				split(x, y, team);
			}
		};

		//What a decoy breaks into: a small blast of its own, nothing else.
		mini = new Warhead("mini", Color.valueOf("ffb347"), 46f, 150f){
			@Override
			public void special(float x, float y, Team team, Unit nuke){
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

	/** The share of its maximum health that a radiation nuke's blast takes from everything in reach at once. */
	public static final float firstDose = 0.15f;

	/** The breach nuke's lane: how far it reaches behind the point and ahead of it, how wide it is, and what it does to a structure and a unit. */
	public static final float laneBack = 700f, laneFront = 120f, laneWidth = 36f, laneDamage = 6000f, laneUnitDamage = 500f;

	/** The way a nuke was flying when it went off. */
	public static float heading(Unit nuke){
		return nuke == null ? 0f : nuke.rotation;
	}

	/** One structure's share of a scramble: a random way round if it turns, otherwise its settings changed or cleared. Server side. */
	public static void scrambleOne(Building b){
		if(!BlockMover.movable(b)) return;
		if(b.block.rotate){
			BlockMover.rebuild(b, Mathf.random(3));
		}else if(b.block.configurable){
			if(b.block.configurations.containsKey(Item.class)) b.configureAny(Vars.content.items().random());
			else if(b.block.configurations.containsKey(Liquid.class)) b.configureAny(Vars.content.liquids().random());
			else b.configureAny(null);
		}
	}

	/** One structure's share of a sabotage: a turret is emptied, and whatever is stored may blow it up. Server side. */
	public static void sabotageOne(Building b){
		float[] stored = {0f, 0f}; //explosiveness, flammability
		boolean turret = b instanceof Turret.TurretBuild;
		if(turret){
			Turret.TurretBuild t = (Turret.TurretBuild)b;
			for(Turret.AmmoEntry entry : t.ammo){
				if(entry instanceof ItemTurret.ItemEntry){
					Item item = ((ItemTurret.ItemEntry)entry).item;
					stored[0] += item.explosiveness * entry.amount * 0.5f;
					stored[1] += item.flammability * entry.amount * 0.5f;
				}
			}
			t.ammo.clear();
			t.totalAmmo = 0;
		}
		if(b.items != null) b.items.each((item, amount) -> {
			stored[0] += item.explosiveness * amount;
			stored[1] += item.flammability * amount;
		});
		if(b.liquids != null) b.liquids.each((liquid, amount) -> {
			stored[0] += liquid.explosiveness * amount;
			stored[1] += liquid.flammability * amount;
		});
		float power = b.block.consPower != null && b.block.consPower.buffered && b.power != null ? b.power.status * b.block.consPower.capacity : 0f;

		boolean dangerous = stored[0] * 3f + stored[1] > 40f || power > 800f;
		if(!dangerous && !turret) return;

		BlockMover.enqueue(() -> {
			if(!b.isValid()) return;
			if(turret && !dangerous){
				//An emptied turret that held nothing dangerous is only shaken.
				b.damagePierce(b.maxHealth * 0.2f);
				return;
			}
			if(turret){
				Damage.dynamicExplosion(b.x, b.y, stored[1], stored[0] * 3.5f, power, b.block.size * tilesize / 2f, true);
				b.damagePierce(b.maxHealth * 0.5f);
			}else{
				//Destroying it sets off its contents the way the game does.
				b.kill();
			}
		});
	}

	/** How many mini nukes a decoy breaks into. */
	public static final int pieces = 3;

	/** Sends {@link #pieces} mini nukes, of the given team, out from the point in all directions. Server side. */
	public static void split(float x, float y, Team team){
		float start = Mathf.random(360f);
		for(int i = 0; i < pieces; i++){
			float a = start + i * 360f / pieces;
			Unit u = NukeUnits.mini.create(team);
			u.set(x + Mathf.cosDeg(a) * 6f, y + Mathf.sinDeg(a) * 6f);
			u.rotation = a;
			u.vel.trns(a, u.type.speed * 0.8f);
			u.controller(u.type.createController(u));
			u.add();
		}
	}

	/** How many lasers the laser nuke fires. */
	public static final int beams = 4;

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

	/** Sets fire to most of the structures within range, whichever side they are on. */
	protected static void burnBuildings(float x, float y, float range){
		Units.nearbyBuildings(x, y, range, b -> {
			if(b.dst(x, y) <= range && Mathf.chance(0.8f)) Fires.create(b.tile);
		});
	}

	/** Spills {@code count} puddles of the liquid at random places within range. */
	/** Puts a puddle of the liquid on every tile within the range, as much as a tile holds. */
	public static int cover(Liquid liquid, float x, float y, float range){
		int[] count = {0};
		arc.math.geom.Geometry.circle(Vars.world.toTile(x), Vars.world.toTile(y), (int)(range / tilesize), (tx, ty) -> {
			Tile tile = Vars.world.tile(tx, ty);
			if(tile == null) return;
			Puddles.deposit(tile, liquid, Mathf.random(30f, 60f));
			count[0]++;
		});
		return count[0];
	}

	protected static void splash(Liquid liquid, int count, float x, float y, float range){
		for(int i = 0; i < count; i++){
			float a = Mathf.random(360f), d = Mathf.sqrt(Mathf.random()) * range;
			Tile tile = Vars.world.tileWorld(x + Mathf.cosDeg(a) * d, y + Mathf.sinDeg(a) * d);
			if(tile != null) Puddles.deposit(tile, liquid, Mathf.random(30f, 70f));
		}
	}

	/** What an EMP destroys outright rather than switching off: batteries, power generators and everything that only moves power about. */
	public static boolean fried(Block block){
		return block instanceof mindustry.world.blocks.power.Battery || block instanceof mindustry.world.blocks.power.PowerGenerator
			|| block instanceof mindustry.world.blocks.power.PowerNode || block instanceof mindustry.world.blocks.power.PowerDiode
			|| block instanceof mindustry.world.blocks.power.BeamNode;
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
