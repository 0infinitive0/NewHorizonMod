package newhorizon.expand.block.omni;

import mindustry.entities.Units;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.Env;
import newhorizon.expand.units.serpent.SerpentChain;

/**
 * Makes the head of a Serpent and its body sections. A team can have only one Serpent, so the head is not started while the team's head
 * is alive; and a section is only started while there is a head to follow, and the team is under its unit cap.
 */
public class SerpentNest extends UnitFactory{
	public SerpentNest(String name){
		super(name);
		envEnabled = Env.any;
		buildType = SerpentNestBuild::new;
	}

	public class SerpentNestBuild extends UnitFactoryBuild{
		@Override
		public boolean shouldConsume(){
			if(!super.shouldConsume()) return false;
			UnitPlan plan = plans.get(currentPlan);
			boolean hasHead = SerpentChain.head(team) != null;
			//The head only while there is none; a section only with a head, and room for it.
			return plan.unit == SerpentChain.body ? hasHead && Units.canCreate(team, plan.unit) : !hasHead;
		}
	}
}
