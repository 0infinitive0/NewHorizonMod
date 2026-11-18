package newhorizon.expand.bullets;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import newhorizon.expand.units.ai.DroneAI;

/**
 * An electric arc that forks. The arc itself goes at what the shooter aims at, as any lightning bullet does; besides that, every shot hits (with a
 * chain bolt and the shot's damage straight away, so it always lands, and electrifying units) the enemy the shooter aims at and up to {@link #forks}
 * more enemies (units, flying or walking, and buildings) within {@link #forkRange} of the shooter, nearest first: three things at once.
 */
public class TeslaForkBulletType extends LightningBulletType{
	public int forks = 2;
	public float forkRange = 115f;
	public float forkStatusTime = 120f;

	@Override
	public void init(Bullet b){
		super.init(b);
		if(Vars.net.client() || !(b.owner instanceof Unit shooter)) return;
		Teamc main = shooter.controller() instanceof DroneAI ai ? ai.chosenTarget() : null;
		fork(b, shooter, main);
	}

	protected void fork(Bullet b, Unit shooter, Teamc main){
		Seq<Teamc> found = new Seq<>();
		float r = forkRange;
		Groups.unit.intersect(b.x - r - 40f, b.y - r - 40f, (r + 40f) * 2f, (r + 40f) * 2f, u -> {
			if(u == main || u == shooter || u.team == b.team || u.team == Team.derelict || !u.isValid() || !u.targetable(b.team)) return;
			if(!u.within(b.x, b.y, r + u.hitSize / 2f)) return;
			if((u.isFlying() && !collidesAir) || (u.isGrounded() && !collidesGround)) return;
			found.add(u);
		});
		if(collidesGround){
			Vars.indexer.allBuildings(b.x, b.y, r + 40f, bd -> {
				if(bd == main || bd.team == b.team || bd.team == Team.derelict || !bd.isValid()) return;
				if(!bd.within(b.x, b.y, r + bd.hitSize() / 2f)) return;
				found.add(bd);
			});
		}
		found.sort(t -> t.dst2(b.x, b.y));
		//The arc itself wanders and can miss a small target, so the one aimed at is hit by a bolt too, if it is in reach.
		Seq<Teamc> hit = new Seq<>();
		if(main != null && !(main instanceof Unit mu && !mu.isValid()) && !(main instanceof Building mb && !mb.isValid()) && main.team() != b.team
			&& main.within(b.x, b.y, r + (main instanceof Unit u ? u.hitSize / 2f : main instanceof Building bd ? bd.hitSize() / 2f : 0f))) hit.add(main);
		for(int i = 0; i < Math.min(forks, found.size); i++) hit.add(found.get(i));
		for(Teamc t : hit){
			Fx.chainLightning.at(b.x, b.y, 0f, lightningColor, t);
			Fx.hitLancer.at(t.getX(), t.getY(), 0f, lightningColor);
			if(t instanceof Unit u){
				u.damage(b.damage);
				u.apply(StatusEffects.electrified, forkStatusTime);
			}else if(t instanceof Building bd){
				bd.damage(b.team, b.damage * buildingDamageMultiplier);
			}
		}
	}
}
