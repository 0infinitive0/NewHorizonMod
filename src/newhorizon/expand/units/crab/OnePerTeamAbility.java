package newhorizon.expand.units.crab;

import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

/**
 * A team can have only one unit of this kind: when there are two, the one that was there first stays and the other is removed at once.
 * Whatever makes the units (a factory, a wave, a map) can ask {@link #count} before it starts.
 */
public class OnePerTeamAbility extends Ability{
	/** How many live units of this type the team has. */
	public static int count(Team team, UnitType type){
		int n = 0;
		for(int i = 0; i < Groups.unit.size(); i++){
			Unit u = Groups.unit.index(i);
			if(u.team == team && u.type == type && !u.dead && u.isAdded()) n++;
		}
		return n;
	}

	/** Whether this unit is the oldest of its type in its team. */
	public static boolean first(Unit unit){
		for(int i = 0; i < Groups.unit.size(); i++){
			Unit u = Groups.unit.index(i);
			if(u != unit && u.team == unit.team && u.type == unit.type && !u.dead && u.isAdded() && u.id < unit.id) return false;
		}
		return true;
	}

	@Override
	public void update(Unit unit){
		if(unit.dead || Vars.net.client()) return;
		if(!first(unit)){
			unit.remove();
			unit.kill();
		}
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-one-per-team");
	}
}
