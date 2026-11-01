package newhorizon.expand.units.ability;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import newhorizon.expand.block.env.IceFloor;

import static mindustry.Vars.tilesize;

/**
 * Thaws the ice around the unit as it sails, so the water is open before the bow reaches it. Ice under a building stays.
 * Only the server changes the floor.
 */
public class IceBreakAbility extends Ability{
	/** Radius in world units. */
	public float radius = 4f * tilesize;
	/** Ticks between two sweeps. */
	public float interval = 5f;
	
	protected float timer;
	
	public IceBreakAbility(float radius){
		this.radius = radius;
	}
	
	@Override
	public void update(Unit unit){
		if(Vars.net.client())return;
		
		timer += Time.delta;
		if(timer < interval)return;
		timer = 0f;
		
		int r = (int)Math.ceil(radius / tilesize);
		int cx = unit.tileX(), cy = unit.tileY();
		for(int dx = -r; dx <= r; dx++){
			for(int dy = -r; dy <= r; dy++){
				Tile tile = Vars.world.tile(cx + dx, cy + dy);
				if(tile == null || !(tile.floor() instanceof IceFloor) || tile.build != null)continue;
				if(!unit.within(tile.worldx(), tile.worldy(), radius))continue;
				((IceFloor)tile.floor()).melt(tile);
			}
		}
	}
	
	@Override
	public void addStats(Table t){
		super.addStats(t);
		t.add(Core.bundle.format("ability.icebreak.range", Strings.autoFixed(radius / tilesize, 1))).wrap().width(320f).left();
	}
	
	@Override
	public String localized(){
		return Core.bundle.get("ability.icebreak");
	}
}
