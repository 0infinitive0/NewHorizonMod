package newhorizon.expand.block.nuke;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mat;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.MinimapRenderer;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.units.nuke.NukeAI;
import newhorizon.expand.units.nuke.NukeDefense;
import newhorizon.expand.units.nuke.Warhead;

/**
 * Puts the nukes a radar has detected on the minimap and on the full map: a ring in the colour of the warhead, the way the nuke is
 * heading, the name of its kind, and (if it has been given one) its target: a line to it and the area the blast covers. It is done by swapping the game's minimap renderer for one that draws these on top of what the
 * game draws, with the same positions and scale, so the same code serves the corner minimap and the full-screen map. A nuke is shown to a
 * team only while one of that team's radars that is working covers it, and it is shown even where the fog of war would hide it.
 */
public class NukeMinimap{
	private static boolean installed;

	/** Swaps the renderer. Client side, once the game is up. */
	public static void install(){
		if(installed || Vars.headless || Vars.renderer == null) return;
		try{
			MinimapRenderer old = Vars.renderer.minimap;
			NukeMinimapRenderer now = new NukeMinimapRenderer();
			now.setZoom(old.getZoom());
			Reflect.set(mindustry.core.Renderer.class, Vars.renderer, "minimap", now);
			if(Vars.world != null && Vars.world.tiles != null && Vars.state != null && Vars.state.isGame()) now.updateAll();
			installed = true;
		}catch(Throwable t){
			//A different version of the game: the minimap simply stays as it is.
			Log.err("The nuke minimap markers could not be installed", t);
		}
	}

	/** Hooks the install to the game having started. */
	public static void hook(){
		if(Vars.headless) return;
		Events.on(EventType.ClientLoadEvent.class, e -> install());
	}

	/** Every active nuke of another team that one of this team's working radars covers. */
	public static Seq<Unit> detected(Team viewer){
		Seq<Unit> found = new Seq<>();
		for(Unit u : Groups.unit){
			if(u.team != viewer && !u.dead() && NukeUnits.isActive(u.type) && NukeDefense.radarSees(viewer, u.x, u.y)) found.add(u);
		}
		return found;
	}

	/** The game's minimap renderer, with the detected nukes drawn on top of it. */
	public static class NukeMinimapRenderer extends MinimapRenderer{
		private final Mat saved = new Mat(), mine = new Mat();
		private Rect rect;

		@Override
		public void drawEntities(float x, float y, float w, float h, boolean fullView){
			super.drawEntities(x, y, w, h, fullView);
			if(Vars.player == null || Vars.world == null || Vars.world.tiles == null) return;
			try{
				Seq<Unit> nukes = detected(Vars.player.team());
				if(nukes.isEmpty()) return;
				if(rect == null) rect = Reflect.get(MinimapRenderer.class, this, "rect");
				mark(nukes, x, y, w, h, fullView);
			}catch(Throwable t){
				//Drawing must never break the map.
			}
		}

		/** Draws the markers under the same transform the game draws units with: world positions in, minimap pixels out. */
		private void mark(Seq<Unit> nukes, float x, float y, float w, float h, boolean fullView){
			saved.set(Draw.trans());
			mine.idt().translate(x, y);
			float scale;
			if(!fullView){
				scale = w / rect.width;
				mine.scl(Tmp.v1.set(scale, h / rect.height));
				mine.translate(-rect.x, -rect.y);
			}else{
				scale = w / Vars.world.unitWidth();
				mine.scl(Tmp.v1.set(scale, h / Vars.world.unitHeight()));
			}
			mine.translate(4f, 4f);
			Draw.trans(mine);

			//World units per pixel of the interface.
			float px = Scl.scl(1f) / scale;
			float pulse = 1f + 0.25f * Mathf.absin(Time.time, 10f, 1f);
			Vec2 goal = new Vec2();
			for(Unit u : nukes){
				Warhead warhead = NukeUnits.warheadOf(u.type);
				Color color = warhead == null ? Color.scarlet : warhead.color;
				//Where it is going, if it knows: a line to the target and the area its blast covers.
				if(NukeAI.targetOf(u, goal)){
					float blast = warhead == null ? 60f : warhead.radius;
					Draw.color(color, 0.14f + 0.06f * Mathf.absin(Time.time, 20f, 1f));
					Fill.circle(goal.x, goal.y, blast);
					Draw.color(color, 0.9f);
					Lines.stroke(2f * px);
					Lines.circle(goal.x, goal.y, blast);
					Lines.line(goal.x - 7f * px, goal.y, goal.x + 7f * px, goal.y);
					Lines.line(goal.x, goal.y - 7f * px, goal.x, goal.y + 7f * px);
					Lines.stroke(1.4f * px);
					Lines.dashLine(u.x, u.y, goal.x, goal.y, Math.max(2, (int)(u.dst(goal) / (14f * px))));
				}
				Draw.color(Color.black, 0.7f);
				Lines.stroke(4.2f * px);
				Lines.circle(u.x, u.y, 8f * px * pulse);
				Draw.color(color);
				Lines.stroke(2.4f * px);
				Lines.circle(u.x, u.y, 8f * px * pulse);
				Lines.lineAngle(u.x, u.y, u.rotation, 20f * px);
				drawLabel(u.x, u.y + 14f * px, u.type.localizedName, color, 1f / scale);
			}
			Draw.reset();
			Draw.trans(saved);
		}
	}
}
