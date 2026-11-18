package newhorizon.expand.liquid;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Puddles;
import mindustry.gen.Puddle;
import mindustry.type.Liquid;
import mindustry.world.Tile;

/**
 * A liquid whose puddles creep. The base game only lets a puddle push a little liquid into the four tiles round it while it is large, and that
 * is the same for every liquid, so a puddle of one that cannot be capped (see {@code capPuddles}) is no wider than oil. Here a puddle that is
 * holding more than {@link #creepMin} sends {@link #creepGive} of it into a random neighbouring tile every few ticks (under buildings too, but not
 * into solid rock), so a big enough pool keeps growing outward, thinning at its edge, until it runs out.
 */
public class CreepingLiquid extends Liquid{
	/** Chance a tick that a puddle creeps, how much it must hold to creep, and how much it sends. */
	public float creepChance = 0.05f, creepMin = 20f, creepGive = 4f;
	/**
	 * The share of the game's evaporation that is given back. The game dries a puddle by (1 - viscosity) / 5 a tick whatever its size, so a pool
	 * that has crept out into many thin puddles would dry up much faster than one that stayed put; giving most of it back keeps it thick and slow to go.
	 */
	public float evaporationRefund = 0.6f;

	private static final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	public CreepingLiquid(String name, Color color){
		super(name, color);
	}

	@Override
	public void update(Puddle puddle){
		super.update(puddle);
		if(Vars.net.client() || puddle == null || puddle.tile == null) return;
		puddle.amount += Time.delta * (1f - viscosity) / 5f * evaporationRefund;
		if(puddle.amount <= creepMin) return;
		if(!Mathf.chance(creepChance * Time.delta)) return;
		int[] d = dirs[Mathf.random(dirs.length - 1)];
		Tile next = Vars.world.tile(puddle.tile.x + d[0], puddle.tile.y + d[1]);
		if(next == null || (next.build == null && next.solid())) return;
		float give = Math.min(creepGive, puddle.amount - creepMin);
		if(give <= 0f) return;
		puddle.amount -= give;
		Puddles.deposit(next, puddle.tile, this, give, false);
	}
}
