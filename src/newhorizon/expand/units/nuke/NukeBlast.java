package newhorizon.expand.units.nuke;

import arc.graphics.Color;
import arc.math.geom.Geometry;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import newhorizon.content.nuke.NukeFx;

import static mindustry.Vars.tilesize;

/** What a nuke does when it dies. */
public class NukeBlast{
	/**
	 * Blows the nuke up where it is. The visuals run everywhere, the damage and everything else only on the server. Damage is dealt by no
	 * team, so it hits everything in reach: enemies, allies and structures of every side, other nukes included.
	 */
	public static void explode(Unit unit, Warhead warhead, boolean active){
		float x = unit.x, y = unit.y;

		if(active){
			warhead.visual(x, y);
			if(warhead.radius < 100f) Effect.shake(3f, 20f, x, y);
			else Effect.shake(warhead.damage > 0f ? 14f : 5f, warhead.damage > 0f ? 90f : 30f, x, y);
			Sounds.explosion.at(x, y, 0.7f, 2.6f);
			if(warhead.damage > 0f) Fx.dynamicExplosion.at(x, y, warhead.radius / 24f);
		}else{
			NukeFx.blast.at(x, y, Warhead.inactiveRadius, Color.valueOf("ffb347"));
			Effect.shake(5f, 30f, x, y);
			Sounds.explosion.at(x, y, 1.1f, 1f);
		}

		//Bullets are simulated on every side, so what a blast leaves behind is made everywhere; the damage and the rest only on the server.
		if(active) warhead.spawn(x, y, unit.team, unit);

		if(Vars.net.client()) return;

		if(active){
			warhead.detonate(x, y, unit.team, unit);
		}else{
			//An inactive nuke's blast is stopped by walls like any explosion.
			Damage.damage(Team.derelict, x, y, Warhead.inactiveRadius, Warhead.inactiveDamage, false, true, true);
			warhead.inactiveSpecial(x, y, unit.team, unit);
		}
	}

	/** A blast of the given size that ignores walls and hurts everything. Server side. */
	public static void blastAt(float x, float y, float radius, float damage, Color color, boolean showEffects){
		if(showEffects){
			NukeFx.blast.at(x, y, radius, color);
			Effect.shake(14f, 90f, x, y);
			Sounds.explosion.at(x, y, 0.7f, 2.6f);
			Fx.dynamicExplosion.at(x, y, radius / 24f);
		}
		if(Vars.net.client()) return;
		Damage.damage(Team.derelict, x, y, radius, damage, true, true, true);
	}

	/** Whether {@code block} can be put on the tile: it is free, the ground takes buildings, and nothing is standing where a solid one would go. */
	public static boolean canFill(Tile tile, Block block){
		if(tile == null || tile.build != null || tile.block() != Blocks.air) return false;
		if(!tile.floor().placeableOn || tile.floor().isDeep()) return false;
		return !(block.solid && Units.anyEntities(tile.worldx() - tilesize / 2f, tile.worldy() - tilesize / 2f, tilesize, tilesize, true));
	}

	/**
	 * Puts {@code block}, on {@code team}, on every free tile within {@code radius} of the point. Server side. Returns how many were placed.
	 * Tiles that something stands on, or that cannot be built on, are left as they are.
	 */
	public static int fill(float x, float y, float radius, Block block, Team team){
		if(Vars.net.client() || block == null) return 0;

		int[] count = {0};
		Geometry.circle(Vars.world.toTile(x), Vars.world.toTile(y), (int)(radius / tilesize), (tx, ty) -> {
			Tile tile = Vars.world.tile(tx, ty);
			if(!canFill(tile, block)) return;
			tile.setNet(block, team, 0);
			count[0]++;
		});
		return count[0];
	}
}
