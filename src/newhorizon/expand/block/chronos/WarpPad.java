package newhorizon.expand.block.chronos;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.IntFloatMap;
import arc.struct.IntIntMap;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import newhorizon.content.chronos.ChronosColor;
import newhorizon.content.chronos.ChronosFx;

import static mindustry.Vars.tilesize;

/**
 * Unit teleporter. A friendly unit that stays on a pad for {@link #chargeTime} ticks is moved to another pad set to the same
 * channel, wherever it is. A unit that has just arrived is not sent on again until it has stepped off the pad it arrived on, so it
 * cannot be bounced back and forth.
 */
public class WarpPad extends ChannelBlock{
	public float chargeTime = 50f;
	public WarpPad(String name){
		super(name);
		solid = false;
		//Everything that stands on it should be able to; it is a floor plate.
		underBullets = true;
	}

	public class WarpPadBuild extends ChannelBuild{
		/** Unit id -> ticks it has stood on the pad. */
		protected final IntFloatMap charging = new IntFloatMap();
		protected final IntIntMap seen = new IntIntMap();
		/** Units that arrived here and have not left yet. */
		protected final IntSet arrivals = new IntSet();
		protected int frame;
		public float glow, spin;

		public float padRadius(){
			return size * tilesize * 0.55f;
		}

		@Override
		public void updateTile(){
			boolean on = efficiency > 0.5f;
			glow = Mathf.lerpDelta(glow, on ? 1f : 0f, 0.05f);
			spin += glow * Time.delta;
			frame++;
			if(!on){
				charging.clear();
				return;
			}

			Seq<ChannelBuild> partners = partners();
			ChannelBuild target = null;
			for(ChannelBuild p : partners){
				if(p.efficiency > 0.5f){
					target = p;
					break;
				}
			}
			if(target == null){
				charging.clear();
				return;
			}

			//Arrivals count as gone once they step off.
			IntSeq left = new IntSeq();
			for(IntSet.IntSetIterator it = arrivals.iterator(); it.hasNext;){
				int id = it.next();
				Unit u = Groups.unit.getByID(id);
				if(u == null || u.dead() || !u.within(this, padRadius())) left.add(id);
			}
			for(int i = 0; i < left.size; i++) arrivals.remove(left.get(i));

			final ChannelBuild dest = target;
			Units.nearby(team, x - padRadius(), y - padRadius(), padRadius() * 2f, padRadius() * 2f, unit -> {
				if(unit.dead() || !unit.within(this, padRadius()) || arrivals.contains(unit.id)) return;

				float t = charging.get(unit.id, 0f) + Time.delta;
				seen.put(unit.id, frame);
				if(t >= chargeTime){
					charging.remove(unit.id, 0f);
					send(unit, dest);
				}else{
					charging.put(unit.id, t);
				}
			});

			//Units that stepped off lose their charge.
			IntSeq gone = new IntSeq();
			for(IntFloatMap.Entry e : charging){
				if(seen.get(e.key, -1) != frame) gone.add(e.key);
			}
			for(int i = 0; i < gone.size; i++) charging.remove(gone.get(i), 0f);
		}

		/** Moves the unit to the other pad. Server side only; clients see it through the next snapshot. */
		public boolean send(Unit unit, ChannelBuild dest){
			if(Vars.net.client()) return false;

			ChronosFx.teleportOut.at(unit.x, unit.y, unit.hitSize * 1.6f);
			unit.set(dest.x, dest.y);
			unit.vel.setZero();
			if(unit.isPlayer()){
				Player player = unit.getPlayer();
				if(player != null) player.set(dest.x, dest.y);
			}
			if(dest instanceof WarpPadBuild) ((WarpPadBuild)dest).arrivals.add(unit.id);
			ChronosFx.teleportOut.at(dest.x, dest.y, unit.hitSize * 1.6f);
			return true;
		}

		/** Whether a unit could be sent right now. */
		public boolean ready(){
			return efficiency > 0.5f;
		}

		@Override
		public void draw(){
			super.draw();
			drawChannel();
			if(glow < 0.01f) return;

			Draw.z(Layer.block + 1f);
			Draw.color(channelColor(), 0.6f * glow);
			Lines.stroke(1.4f);
			Lines.circle(x, y, padRadius());
			Lines.arc(x, y, padRadius() * 0.7f, 0.3f, spin * 2f);
			Lines.arc(x, y, padRadius() * 0.7f, 0.3f, spin * 2f + 180f);
			float best = 0f;
			for(IntFloatMap.Entry e : charging) best = Math.max(best, e.value);
			if(best > 0f){
				Draw.color(ChronosColor.chronoLight, 0.9f);
				Lines.stroke(2.2f);
				Lines.arc(x, y, padRadius() * 1.15f, best / chargeTime, 90f);
			}
			Draw.reset();
		}
	}
}
