package newhorizon.expand.block.omni;

import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.Env;
import newhorizon.expand.units.serpent.SerpentChain;

/**
 * Makes the head of a Serpent. A team can have only one Serpent, so it will not start another while the team's head is alive.
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
			return super.shouldConsume() && SerpentChain.head(team) == null;
		}
	}
}
