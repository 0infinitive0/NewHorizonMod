package newhorizon.expand.units.nuke;

import arc.Core;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ai.types.CommandAI;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Door;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BuildVisibility;
import newhorizon.content.nuke.NukeUnits;

/**
 * Makes a unit a nuke. It does nothing until it dies: whatever kills it (a shot, a command, running out of fuel, running into a
 * target) it explodes, see {@link NukeBlast}. An active nuke also detonates when its pilot fires, and burns out after {@link #fuel}
 * ticks. An inactive one cannot be detonated on purpose, only destroyed.
 */
public class NukeAbility extends Ability{
	public Warhead warhead;
	public boolean active;
	/** Ticks an active nuke flies before it detonates by itself. */
	public float fuel = 60f * 45f;

	protected float age;
	protected WallStance appliedStance;

	/** Which wall a wall nuke fills with: kept in the ability's data (which is saved with the unit) as the block's id; 0 means the default. */
	public Block wallType(){
		Block block = data > 0f ? Vars.content.block((int)data) : null;
		return isWall(block) ? block : defaultWall();
	}

	public void wallType(Block block){
		if(isWall(block)) data = block.id;
	}

	public static Block defaultWall(){
		return Blocks.copperWall;
	}

	/** Only plain one-tile walls can be chosen. */
	public static boolean isWall(Block block){
		return block instanceof Wall && block.size == 1;
	}

	/** The walls offered as stances: one-tile walls that can be built, without doors (a door is not a wall to fill a field with). */
	public static boolean choosable(Block block){
		return isWall(block) && !(block instanceof Door) && block.buildVisibility == BuildVisibility.shown;
	}

	public NukeAbility(Warhead warhead, boolean active){
		this.warhead = warhead;
		this.active = active;
	}

	@Override
	public void update(Unit unit){
		if(warhead == Warhead.wall && !active) syncWallStance(unit);
		if(!active || Vars.net.client()) return;

		age += Time.delta;
		if(age >= fuel || (unit.isPlayer() && unit.isShooting)) detonate(unit);
	}

	/**
	 * The wall is picked with the stance buttons of the selected unit. When a different one is switched on, the nuke takes it. Between
	 * switches anything else may change the wall, so this does not hold the nuke to the stance.
	 */
	protected void syncWallStance(Unit unit){
		if(!(unit.controller() instanceof CommandAI)) return;
		CommandAI commander = (CommandAI)unit.controller();
		for(WallStance stance : NukeUnits.wallStances){
			if(commander.hasStance(stance)){
				if(stance != appliedStance){
					appliedStance = stance;
					wallType(stance.block);
				}
				return;
			}
		}
		appliedStance = null;
	}

	/** Detonates an active nuke. Nothing for an inactive one. */
	public boolean detonate(Unit unit){
		if(!active || unit.dead() || !unit.isAdded()) return false;
		unit.kill();
		return true;
	}

	@Override
	public void death(Unit unit){
		NukeBlast.explode(unit, warhead, active);
	}

	@Override
	public Ability copy(){
		//Warhead and mode are shared by every copy; only the fuel clock is per unit.
		NukeAbility copy = (NukeAbility)super.copy();
		copy.age = 0f;
		return copy;
	}

	@Override
	public String localized(){
		return Core.bundle.get(active ? "ability.nh-nuke-active" : "ability.nh-nuke-inactive", "Nuke");
	}
}
