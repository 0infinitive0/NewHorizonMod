package newhorizon.expand.block.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Damage;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.NukeFx;

/**
 * A mine, laid by cluster nukes. It does nothing until an enemy ground unit stands on it or it is destroyed by anything, and then it
 * explodes, hurting enemies of its team around it. It costs nothing while it waits: it does not update, the game tells it when a
 * unit is on it. Anything can be built over it.
 */
public class MineBlock extends Block{
	public float blastRadius = 38f, blastDamage = 130f;

	public MineBlock(String name){
		super(name);
		size = 1;
		update = false;
		solid = false;
		destructible = true;
		breakable = true;
		alwaysReplace = true;
		rebuildable = false;
		underBullets = true;
		hasShadow = false;
		createRubble = false;
		priority = mindustry.entities.TargetPriority.under;
		envEnabled = Env.any;
	}

	public class MineBuild extends Building{
		/** Whether this unit would set the mine off: an enemy that walks. */
		public boolean triggeredBy(Unit unit){
			return unit.team != team && unit.team != Team.derelict && !unit.dead();
		}

		@Override
		public void unitOn(Unit unit){
			if(Vars.net.client() || !triggeredBy(unit)) return;
			kill();
		}

		@Override
		public void onDestroyed(){
			//The mine's own blast, not the generic one a destroyed block makes.
			NukeFx.blast.at(x, y, blastRadius, Color.valueOf("ff9f43"));
			if(Vars.net.client()) return;
			Damage.damage(team, x, y, blastRadius, blastDamage, false, true, true);
		}

		@Override
		public void draw(){
			super.draw();
			Draw.z(Layer.block + 0.2f);
			Draw.color(Pal.remove, 0.5f + 0.5f * Mathf.absin(Time.time + id * 7f, 40f, 1f));
			Fill.circle(x, y, 1.1f);
			Draw.reset();
		}
	}
}
