package newhorizon.expand.units.serpent;

import arc.Core;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.world.blocks.storage.CoreBlock;
import newhorizon.content.nuke.NukeItems;

/**
 * The Add section command of a Serpent's head: one omni-matter payment from the core buys one more body section at the tail, and the head
 * goes back to Move. It does nothing if the core cannot pay or the team is at its unit cap.
 */
public class SerpentGrowAI extends AIController{
	/** What a section costs, in omni-matter from the core. */
	public static int cost = 100;
	/** Ticks that have to pass between two sections of the same team. */
	public static float cooldown = 30f;

	private static final arc.struct.IntFloatMap last = new arc.struct.IntFloatMap();

	@Override
	public void updateMovement(){
		if(Vars.net.client() || unit == null) return;
		grow(unit.team, true);
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			commander.targetPos = null;
			commander.attackTarget = null;
			commander.command(UnitCommand.moveCommand);
		}
	}

	/** Nothing to aim. */
	@Override
	public void updateTargeting(){
	}

	/** Adds a section to the team's Serpent, if it can pay. Returns the new section, or null. Server side. */
	public static Unit grow(Team team, boolean tell){
		Unit tail = SerpentChain.tail(team);
		if(tail == null) return null;
		if(arc.util.Time.time - last.get(team.id, -1000f) < cooldown) return null;

		CoreBlock.CoreBuild core = team.core();
		boolean free = Vars.state.rules.infiniteResources;
		if(!free && (core == null || !core.items.has(NukeItems.omniMatter, cost))){
			if(tell) say(team, "nh.serpent.noitems");
			return null;
		}
		if(!Units.canCreate(team, SerpentChain.body)){
			if(tell) say(team, "nh.serpent.cap");
			return null;
		}

		if(!free) core.items.remove(NukeItems.omniMatter, cost);
		last.put(team.id, arc.util.Time.time);

		Unit section = SerpentChain.body.create(team);
		float spacing = SerpentChain.spacing(section, tail);
		section.set(tail.x + arc.math.Angles.trnsx(tail.rotation + 180f, spacing), tail.y + arc.math.Angles.trnsy(tail.rotation + 180f, spacing));
		section.rotation = tail.rotation;
		section.add();
		SerpentChain.refresh();
		Fx.spawn.at(section.x, section.y);
		return section;
	}

	private static void say(Team team, String key){
		if(!Vars.headless && Vars.player != null && Vars.player.team() == team) Vars.ui.showInfoToast(Core.bundle.get(key), 3f);
	}
}
