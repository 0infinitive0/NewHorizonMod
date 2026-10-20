package newhorizon.expand.units.nuke;

import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import newhorizon.expand.block.nuke.DefenseField;

/**
 * What the defence blocks do to nukes, looked up by the nukes' AI and the launchers. A field only counts for a nuke or launcher of
 * another team than its own, and only while it is working.
 */
public class NukeDefense{
	/** Further than any field reaches: how far to look for one. */
	public static final float scan = 700f;

	/** The first working field of this kind that reaches the point and does not belong to {@code mine}. */
	public static DefenseField.DefenseFieldBuild field(DefenseField.Kind kind, Team mine, float x, float y){
		DefenseField.DefenseFieldBuild[] found = {null};
		Units.nearbyBuildings(x, y, scan, b -> {
			if(found[0] != null || !(b instanceof DefenseField.DefenseFieldBuild)) return;
			DefenseField.DefenseFieldBuild f = (DefenseField.DefenseFieldBuild)b;
			if(f.kind() == kind && f.team != mine && f.team != Team.derelict && f.active() && f.within(x, y, f.range())) found[0] = f;
		});
		return found[0];
	}

	/** Whether a launcher of this team may not target the point, because an enemy cloak hides it. */
	public static boolean cloaked(Team attacker, float x, float y){
		return field(DefenseField.Kind.cloak, attacker, x, y) != null;
	}

	/** The enemy beacon that is pulling this nuke in, or null. */
	public static Building lure(Unit nuke){
		return field(DefenseField.Kind.beacon, nuke.team, nuke.x, nuke.y);
	}

	/** Whether an enemy jammer reaches this nuke. */
	public static boolean jammed(Unit nuke){
		return field(DefenseField.Kind.jammer, nuke.team, nuke.x, nuke.y) != null;
	}
}
