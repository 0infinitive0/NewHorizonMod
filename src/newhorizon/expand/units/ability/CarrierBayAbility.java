package newhorizon.expand.units.ability;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Call;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import newhorizon.expand.units.ai.CarrierFighterAI;

import static mindustry.Vars.tilesize;

/**
 * The hangar of a carrier. It builds fighters into a store, launches them when an enemy comes within its (very long)
 * range, and takes them back when there is no enemy left or they are hurt. A fighter that is taken in comes out whole.
 * The state is not saved: after loading, the hangar fills up again.
 */
public class CarrierBayAbility extends Ability{
	public UnitType fighter;
	/** Fighters in the air and in the hangar together. */
	public int maxFighters = 6;
	/** Fighters in the hangar when the carrier is made. */
	public int startStored = 2;
	/** Ticks to build one fighter. */
	public float buildTime = 7f * 60f;
	/** Ticks between two launches. */
	public float launchInterval = 20f;
	/** Range in world units: enemies this close to the carrier send the fighters out. */
	public float range = 70f * tilesize;
	/** A fighter this close to the carrier is taken in. */
	public float dockRange = 3.5f * tilesize;
	/** Where the fighters come out, in the frame of the carrier (x to the right, y to the front). */
	public float bayX = 0f, bayY = -14f;
	/** Health fraction under which a fighter is called back. */
	public float recallHealth = 0.35f;
	
	public final Seq<Unit> launched = new Seq<>();
	public int stored;
	/** Whether an enemy is in range. */
	public boolean engaged;
	
	protected float buildTimer, launchTimer, scanTimer;
	protected boolean started;
	
	public CarrierBayAbility(UnitType fighter, int maxFighters, float buildTime, float range){
		this.fighter = fighter;
		this.maxFighters = maxFighters;
		this.buildTime = buildTime;
		this.range = range;
	}
	
	@Override
	public void update(Unit unit){
		if(Vars.net.client())return;
		
		if(!started){
			started = true;
			stored = Math.min(startStored, maxFighters);
		}
		
		launched.removeAll(u -> u == null || u.dead || !u.isAdded());
		
		scanTimer += Time.delta;
		if(scanTimer >= 15f){
			scanTimer = 0f;
			Teamc enemy = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> true, b -> true);
			engaged = enemy != null;
		}
		
		if(launched.size + stored < maxFighters){
			buildTimer += Time.delta;
			if(buildTimer >= buildTime){
				buildTimer = 0f;
				stored++;
			}
		}
		
		if(engaged && stored > 0){
			launchTimer += Time.delta;
			if(launchTimer >= launchInterval){
				launchTimer = 0f;
				launch(unit);
			}
		}
	}
	
	protected void launch(Unit carrier){
		Unit out = fighter.create(carrier.team);
		Tmp.v1.set(bayX, bayY).rotate(carrier.rotation - 90f);
		out.set(carrier.x + Tmp.v1.x, carrier.y + Tmp.v1.y);
		out.rotation = carrier.rotation;
		out.controller(new CarrierFighterAI(carrier, this));
		out.add();
		stored--;
		launched.add(out);
	}
	
	/** Takes a fighter into the hangar. */
	public void dock(Unit unit){
		if(Vars.net.client() || !launched.remove(unit))return;
		stored++;
		Call.unitDespawn(unit);
	}
	
	@Override
	public void addStats(Table t){
		super.addStats(t);
		t.add(Core.bundle.format("ability.carrierbay.info", maxFighters, Strings.autoFixed(range / tilesize, 0), fighter.localizedName)).wrap().width(360f).left();
	}
	
	@Override
	public String localized(){
		return Core.bundle.get("ability.carrierbay");
	}
}
