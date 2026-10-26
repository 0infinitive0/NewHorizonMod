package newhorizon.expand.units.nuke;

import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.storage.CoreBlock;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.block.nuke.NukeLauncher;

/**
 * What an AI team's nukes do on their own: where to aim ({@link #pick}), which launcher to fly into ({@link #freeLauncher}) and when a
 * launcher fires ({@link #driveLauncher}). A target is chosen by what the warhead is good at: structure kinds look for the thickest group
 * of structures (a core counts for six, a turret for three), unit kinds for the biggest group of units, the EMP for the power network and
 * flying units, and a few kinds (walls, neoplasm, the decoy) just go for the nearest core. A nuke hurts everyone, so what is the nuke's own
 * team's in the same circle counts against a target, three times over, and nothing under an enemy cloak is picked.
 */
public class NukeBrain{
	public enum Kind{
		structures, units, power, core
	}

	/** What a target has to be worth, for structure and power kinds (points) and unit kinds (total health), before a nuke is spent on it. */
	public static float minStructureScore = 6f, minUnitHealth = 600f;
	/** How many places are tried for one choice, how much of the score a far target loses at most, and how much an own thing counts against one. */
	public static int candidates = 80;
	public static float distancePenalty = 0.33f, ownWeight = 3f;
	/** How far from itself an inactive nuke looks for a launcher, and how long a Multi launcher waits for a fuller magazine. */
	public static float launcherReach = 50f * 8f, magazineWait = 60f * 30f;

	public static Kind kindOf(Warhead warhead){
		if(warhead == Warhead.psychic || warhead == Warhead.gravity || warhead == Warhead.spatial || warhead == Warhead.temporal
			|| warhead == Warhead.infantry || warhead == Warhead.overload) return Kind.units;
		if(warhead == Warhead.emp) return Kind.power;
		if(warhead == Warhead.wall || warhead == Warhead.neoplasm || warhead == Warhead.decoy || warhead == Warhead.mini) return Kind.core;
		return Kind.structures;
	}

	private static boolean enemy(Team team, Team other){
		return other != team && other != Team.derelict;
	}

	/** What a structure is worth to a kind of warhead. */
	static float weight(Kind kind, Building b){
		if(kind == Kind.units) return 0f;
		if(kind == Kind.power){
			if(Warhead.fried(b.block)) return 3f;
			return b.power != null && b.block.consPower != null ? 1f : 0f;
		}
		if(b instanceof CoreBlock.CoreBuild) return 6f;
		if(b.block instanceof Turret) return 3f;
		return 1f;
	}

	/** What a unit is worth to a kind of warhead: its health for unit kinds, 3 for a flyer under an EMP. */
	static float weight(Kind kind, Unit u){
		if(u.dead() || NukeUnits.isNuke(u.type)) return 0f;
		if(kind == Kind.units) return u.health;
		if(kind == Kind.power) return u.isFlying() ? 3f : 0f;
		return 0f;
	}

	/** How much a circle is worth to the nuke of this team: what is the enemy's in it, less three times what is its own. */
	public static float score(Team team, Kind kind, float x, float y, float radius){
		float[] total = {0f};
		Units.nearbyBuildings(x, y, radius, b -> {
			if(!b.isValid() || b.dst(x, y) > radius || b.team == Team.derelict) return;
			float w = weight(kind, b);
			total[0] += b.team == team ? -w * ownWeight : w;
		});
		Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
			if(!u.within(x, y, radius + u.hitSize / 2f) || u.team == Team.derelict) return;
			float w = weight(kind, u);
			total[0] += u.team == team ? -w * ownWeight : w;
		});
		return total[0];
	}

	/** The nearest enemy core, as a point; null if there is none. */
	public static Vec2 nearestCore(Team team, Position from){
		CoreBlock.CoreBuild core = Vars.state.teams.closestEnemyCore(from.getX(), from.getY(), team);
		return core == null ? null : new Vec2(core.x, core.y);
	}

	/**
	 * Where a nuke of this warhead and team should go, from {@code from}: the best scoring place among some of the enemy's structures or
	 * units, or the nearest enemy core for the kinds that only need one. Null if nothing is worth a nuke. Server side.
	 */
	public static Vec2 pick(Team team, Warhead warhead, Position from){
		Kind kind = kindOf(warhead);
		if(kind == Kind.core){
			Vec2 core = nearestCore(team, from);
			return core == null || NukeDefense.cloaked(team, core.x, core.y) ? null : core;
		}

		Seq<Position> options = new Seq<>();
		if(kind != Kind.units){
			//Every structure of every enemy team (blocks that never update are not in Groups.build, so the teams' own lists are used).
			for(mindustry.game.Teams.TeamData data : Vars.state.teams.present){
				if(!enemy(team, data.team)) continue;
				for(Building b : data.buildings) if(b.isValid() && weight(kind, b) > 0f) options.add(b);
			}
		}
		if(kind != Kind.structures){
			for(Unit u : Groups.unit) if(enemy(team, u.team) && weight(kind, u) > 0f) options.add(u);
		}
		if(options.isEmpty()) return null;
		options.shuffle();

		float minimum = kind == Kind.units ? minUnitHealth : minStructureScore;
		float diag = Math.max(1f, Vars.world.unitWidth() + Vars.world.unitHeight());
		Vec2 best = null;
		float bestValue = 0f;
		for(int i = 0; i < options.size && i < candidates; i++){
			Position p = options.get(i);
			if(NukeDefense.cloaked(team, p.getX(), p.getY())) continue;
			float s = score(team, kind, p.getX(), p.getY(), warhead.radius);
			if(s < minimum) continue;
			float value = s * (1f - distancePenalty * Math.min(1f, (float)Math.sqrt(from.getX() == p.getX() && from.getY() == p.getY() ? 0f : Math.pow(from.getX() - p.getX(), 2) + Math.pow(from.getY() - p.getY(), 2)) / diag));
			if(value > bestValue){
				bestValue = value;
				best = new Vec2(p.getX(), p.getY());
			}
		}
		return best;
	}

	/** The launcher of the nuke's own team that it should fly into, or null: Orbital first, then Multi, then the plain one, within reach and with room. */
	public static NukeLauncher.NukeLauncherBuild freeLauncher(Unit nuke){
		NukeLauncher.NukeLauncherBuild[] best = {null};
		int[] rank = {-1};
		Units.nearbyBuildings(nuke.x, nuke.y, launcherReach + 60f, b -> {
			if(!(b instanceof NukeLauncher.NukeLauncherBuild) || b.team != nuke.team || !b.isValid() || b.dst(nuke) > launcherReach) return;
			NukeLauncher.NukeLauncherBuild l = (NukeLauncher.NukeLauncherBuild)b;
			if(!l.acceptUnitPayload(nuke)) return;
			int r = b.block instanceof newhorizon.expand.block.nuke.OrbitalLauncher ? 3 : b.block instanceof newhorizon.expand.block.nuke.MultiNukeLauncher ? 2 : 1;
			if(r > rank[0] || (r == rank[0] && b.dst(nuke) < best[0].dst(nuke))){
				rank[0] = r;
				best[0] = l;
			}
		});
		return best[0];
	}

	/**
	 * What an AI team's launcher does about once a second: with a nuke loaded, choose a target and give the order to arm and launch, which the
	 * launcher carries out by itself. A single launcher goes as soon as there is a target worth a nuke; a magazine waits until it is half full,
	 * or until {@link #magazineWait} has passed since the last nuke came in. Server side; a player's launcher is never touched.
	 */
	public static void driveLauncher(NukeLauncher.NukeLauncherBuild b){
		if(Vars.net.client() || !b.team.isAI()) return;
		int count = b.count();
		if(count != b.aiCount){
			b.aiCount = count;
			b.aiSince = Time.time;
		}
		if(!b.loaded() || b.salvoing() || (b.hasTarget && b.launchOrder)) return;
		if(Time.time < b.aiNext) return;
		b.aiNext = Time.time + 60f;

		int capacity = b.capacityNow();
		if(capacity > 1 && count * 2 < capacity && Time.time - b.aiSince < magazineWait) return;
		Unit first = b.loadedUnit();
		Warhead warhead = first == null ? null : NukeUnits.warheadOf(first.type);
		if(warhead == null) return;
		Vec2 target = pick(b.team, warhead, b);
		if(target == null) return;
		b.setTarget(target);
		if(!b.hasTarget) return;
		b.armSwitch = true;
		b.launchOrder = true;
	}
}
