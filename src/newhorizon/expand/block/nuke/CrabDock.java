package newhorizon.expand.block.nuke;

import mindustry.entities.Units;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.CrabUnits;
import newhorizon.expand.units.crab.OnePerTeamAbility;

/** Makes the units a team can have only one of (the King Crab, the Gravity Base): it does not start another while the team's is alive. */
public class CrabDock extends UnitFactory{
	public CrabDock(String name){
		super(name);
		envEnabled = Env.any;
		buildType = CrabDockBuild::new;
	}

	public class CrabDockBuild extends UnitFactoryBuild{
		@Override
		public boolean shouldConsume(){
			if(!super.shouldConsume()) return false;
			UnitPlan plan = plans.get(currentPlan);
			return OnePerTeamAbility.count(team, plan.unit) == 0 && Units.canCreate(team, plan.unit);
		}
	}
}
