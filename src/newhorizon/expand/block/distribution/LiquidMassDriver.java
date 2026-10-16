package newhorizon.expand.block.distribution;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.OrderedSet;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.MassDriver.DriverState;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import arc.audio.Sound;
import newhorizon.expand.bullets.LiquidDriverBolt;
import newhorizon.expand.bullets.LiquidDriverBolt.LiquidDriverData;

import static mindustry.Vars.control;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

/**
 * The liquid counterpart of the mass driver. Link two of them and the first fires its stored liquid across the gap in bolts that the
 * second pours out, with no conduit in between: over walls, cliffs, deep water or enemy lines.
 *
 * <p>The behaviour follows the vanilla mass driver, so drivers can be chained into a relay:
 * <ul>
 *     <li>A driver with a link takes liquid from conduits and fires it at its link once it holds {@link #minDistribute} and the
 *     receiver has that much room. The receiver turns to face the shooter first; several shooters queue up and fire in turn.</li>
 *     <li>A driver nobody links to, or with no link of its own, dumps liquid into adjacent conduits and containers.</li>
 *     <li>Only one liquid travels at a time: a receiver that is holding a different liquid is not fired at.</li>
 *     <li>Liquid that cannot be delivered (the target was destroyed, or something else filled it in the meantime) splashes on the
 *     ground, it is never silently lost nor duplicated.</li>
 * </ul>
 */
public class LiquidMassDriver extends Block{
	/** Maximum distance to the linked driver. */
	public float range = 440f;
	public float rotateSpeed = 0.04f;
	/** Distance from the centre where a bolt leaves. */
	public float translation = 7f;
	/** Least amount of liquid worth firing, also the room a receiver must have. */
	public float minDistribute = 100f;
	/** Most liquid in one bolt. */
	public float payload = 600f;
	public float knockback = 4f;
	public float reload = 120f;
	public LiquidDriverBolt bullet = new LiquidDriverBolt();
	public float bulletSpeed = 5.5f;
	public float bulletLifetime = 200f;
	/** A bolt may fill the receiver up to this multiple of its capacity, like vanilla. */
	public float overfill = 1.25f;
	public Effect shootEffect = Fx.shootBig2;
	public Effect smokeEffect = Fx.shootBigSmoke2;
	public Effect receiveEffect = Fx.mineBig;
	public Sound shootSound = Sounds.shootBig;
	public float shake = 2f;

	public TextureRegion baseRegion, liquidRegion;

	public LiquidMassDriver(String name){
		super(name);
		update = true;
		solid = true;
		configurable = true;
		hasLiquids = true;
		outputsLiquid = true;
		hasPower = true;
		outlineIcon = true;
		sync = true;
		saveConfig = true;
		group = BlockGroup.liquids;
		rotate = false;
		envEnabled |= Env.space;
		liquidCapacity = 1800f;

		//relative position when copy-pasted, absolute position from a link
		config(Point2.class, (LiquidMassDriverBuild tile, Point2 point) -> tile.link = Point2.pack(point.x + tile.tileX(), point.y + tile.tileY()));
		config(Integer.class, (LiquidMassDriverBuild tile, Integer point) -> tile.link = point);
	}

	@Override
	public void init(){
		bullet.fullAmount = payload;
		super.init();
	}

	@Override
	public void load(){
		super.load();
		baseRegion = Core.atlas.find(name + "-base");
		liquidRegion = Core.atlas.find(name + "-liquid");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
		stats.add(Stat.output, payload, StatUnit.liquidUnits);
		stats.add(Stat.reload, 60f / reload, StatUnit.perSecond);
	}

	@Override
	public TextureRegion[] icons(){
		return new TextureRegion[]{baseRegion, region};
	}

	@Override
	public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
		Draw.rect(baseRegion, plan.drawx(), plan.drawy());
		Draw.rect(region, plan.drawx(), plan.drawy());
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.accent);

		//check if a mass driver is selected while placing this driver
		if(!control.input.config.isShown()) return;
		Building selected = control.input.config.getSelected();
		if(selected == null || selected.block != this || !selected.within(x * tilesize + offset, y * tilesize + offset, range + size * tilesize / 2f)) return;

		//if so, draw a dotted line towards it while it is in range
		float sin = Mathf.absin(Time.time, 6f, 1f);
		Tmp.v1.set(x * tilesize + offset, y * tilesize + offset).sub(selected.x, selected.y).limit((size / 2f + 1) * tilesize + sin + 0.5f);
		float x2 = x * tilesize - Tmp.v1.x, y2 = y * tilesize - Tmp.v1.y,
			x1 = selected.x + Tmp.v1.x, y1 = selected.y + Tmp.v1.y;
		int segs = (int)(selected.dst(x * tilesize, y * tilesize) / tilesize);

		Lines.stroke(4f, Pal.gray);
		Lines.dashLine(x1, y1, x2, y2, segs);
		Lines.stroke(2f, Pal.placing);
		Lines.dashLine(x1, y1, x2, y2, segs);
		Draw.reset();
	}

	public class LiquidMassDriverBuild extends Building{
		/** Packed position of the driver this one fires at, -1 when none. */
		public int link = -1;
		public float rotation = 90;
		public float reloadCounter = 0f;
		public DriverState state = DriverState.idle;
		/** Drivers waiting to fire at this one, in order. */
		public OrderedSet<LiquidMassDriverBuild> waitingShooters = new OrderedSet<>();

		public LiquidMassDriverBuild currentShooter(){
			return waitingShooters.isEmpty() ? null : waitingShooters.first();
		}

		/** Room for more liquid, counting only what fits up to the normal capacity. */
		public float freeSpace(){
			return Math.max(liquidCapacity - liquids.currentAmount(), 0f);
		}

		/** Whether liquid of this kind could be put in without mixing. */
		public boolean holdsOnly(Liquid liquid){
			return liquids.currentAmount() < 0.2f || liquids.current() == liquid;
		}

		/** Whether a bolt of the liquid this driver holds could be fired at the other driver right now. */
		public boolean canSend(LiquidMassDriverBuild other){
			return liquids.currentAmount() >= minDistribute
				&& other.freeSpace() >= minDistribute
				&& other.holdsOnly(liquids.current());
		}

		@Override
		public void updateTile(){
			//the link may have been replaced since it was set
			LiquidMassDriverBuild linked = linkedDriver();
			boolean hasLink = linked != null;

			if(hasLink){
				link = linked.pos();
			}

			//reload regardless of state
			if(reloadCounter > 0f){
				reloadCounter = Mathf.clamp(reloadCounter - edelta() / reload);
			}

			LiquidMassDriverBuild current = currentShooter();

			//forget shooters that are gone or not aimed at this driver any more
			if(current != null && !shooterValid(current)){
				waitingShooters.remove(current);
			}

			//switch states
			if(state == DriverState.idle){
				//start accepting when idle and there is room
				if(!waitingShooters.isEmpty() && freeSpace() >= minDistribute){
					state = DriverState.accepting;
				}else if(hasLink){ //otherwise shoot if there is somewhere to shoot at
					state = DriverState.shooting;
				}
			}

			//pass liquid on to whatever is next to this driver while it has nothing else to do
			if(state == DriverState.idle || state == DriverState.accepting){
				dumpLiquid(liquids.current());
			}

			//nothing works without power
			if(efficiency <= 0f){
				return;
			}

			if(state == DriverState.accepting){
				//bail if nothing is shooting at this any more, or it is full
				if(currentShooter() == null || freeSpace() < minDistribute){
					state = DriverState.idle;
					return;
				}

				//face the shooter
				rotation = Mathf.slerpDelta(rotation, tile.angleTo(currentShooter()), rotateSpeed * efficiency);
			}else if(state == DriverState.shooting){
				//bail if there is nothing to shoot at, or someone wants to shoot at this
				if(!hasLink || (!waitingShooters.isEmpty() && freeSpace() >= minDistribute)){
					state = DriverState.idle;
					return;
				}

				float targetRotation = tile.angleTo(linked);

				if(canSend(linked)){
					linked.waitingShooters.add(this);

					if(reloadCounter <= 0.0001f){
						//aim at the target
						rotation = Mathf.slerpDelta(rotation, targetRotation, rotateSpeed * efficiency);

						//fire when first in its queue and both ends are lined up
						if(linked.currentShooter() == this
							&& linked.state == DriverState.accepting
							&& Angles.near(rotation, targetRotation, 2f)
							&& Angles.near(linked.rotation, targetRotation + 180f, 2f)){
							fire(linked);
							float timeToArrive = Math.min(bulletLifetime, dst(linked) / bulletSpeed);
							Time.run(timeToArrive, () -> {
								//done firing, let the next one in
								linked.waitingShooters.remove(this);
								linked.state = DriverState.idle;
							});
							//this driver is free again straight away
							state = DriverState.idle;
						}
					}
				}else if(reloadCounter <= 0.0001f){
					//cannot send (out of liquid, or the receiver is full or holds something else): do not hold up the receiver
					linked.waitingShooters.remove(this);
				}
			}
		}

		@Override
		public void draw(){
			Draw.rect(baseRegion, x, y);

			//what is stored, in the open tank under the barrel
			if(liquidRegion.found() && liquids.currentAmount() > 0.01f){
				Drawf.liquid(liquidRegion, x, y, Mathf.clamp(liquids.currentAmount() / liquidCapacity), liquids.current().color);
			}

			Draw.z(Layer.turret);

			Drawf.shadow(region,
				x + Angles.trnsx(rotation + 180f, reloadCounter * knockback) - (size / 2f),
				y + Angles.trnsy(rotation + 180f, reloadCounter * knockback) - (size / 2f), rotation - 90);
			Draw.rect(region,
				x + Angles.trnsx(rotation + 180f, reloadCounter * knockback),
				y + Angles.trnsy(rotation + 180f, reloadCounter * knockback), rotation - 90);
		}

		@Override
		public void drawConfigure(){
			float sin = Mathf.absin(Time.time, 6f, 1f);

			Lines.stroke(1f);
			Drawf.circles(x, y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.accent);

			for(LiquidMassDriverBuild shooter : waitingShooters){
				Drawf.circles(shooter.x, shooter.y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
				Drawf.arrow(shooter.x, shooter.y, x, y, size * tilesize + sin, 4f + sin, Pal.place);
			}

			LiquidMassDriverBuild target = linkedDriver();
			if(target != null){
				Drawf.circles(target.x, target.y, (target.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
				Drawf.arrow(x, y, target.x, target.y, size * tilesize + sin, 4f + sin);
			}

			Drawf.dashCircle(x, y, range, Pal.accent);
		}

		@Override
		public boolean onConfigureBuildTapped(Building other){
			//reverse the connection when tapped twice
			if(this == other){
				if(link == -1){
					deselect();
				}else{
					configure(-1);
				}
				return false;
			}

			if(link == other.pos()){
				configure(-1);
				return false;
			}else if(other.block == block && other.dst(tile) <= range && other.team == team){
				configure(other.pos());
				return false;
			}

			return true;
		}

		/** Only a driver with a working link takes liquid from conduits, otherwise it is only an outlet. */
		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return linkValid() && holdsOnly(liquid) && liquids.currentAmount() < liquidCapacity - 0.001f;
		}

		protected void fire(LiquidMassDriverBuild target){
			Liquid liquid = liquids.current();
			float amount = Math.min(Math.min(liquids.currentAmount(), payload), target.freeSpace());
			liquids.remove(liquid, amount);

			LiquidDriverData data = new LiquidDriverData(this, target, liquid, amount);

			float angle = tile.angleTo(target);

			bullet.create(this, team,
				x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation),
				angle, -1f, bulletSpeed, bulletLifetime, data);

			shootEffect.at(x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation), angle, liquid.color);
			smokeEffect.at(x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation), angle, liquid.color);

			Effect.shake(shake, shake, this);
			if(shootSound != null) shootSound.at(this, Mathf.random(0.9f, 1.1f));

			reloadCounter = 1f;
		}

		/** Called by a bolt that arrived. Pours in what fits and leaves the rest in the bolt, which splashes it. */
		public void handlePayload(Bullet bullet, LiquidDriverData data){
			//a driver that has been removed takes nothing: the bolt splashes it all
			if(isValid() && holdsOnly(data.liquid)){
				float room = Math.max(liquidCapacity * overfill - liquids.currentAmount(), 0f);
				float add = Math.min(data.amount, room);
				if(add > 0f){
					liquids.add(data.liquid, add);
					data.amount -= add;
					data.delivered += add;
				}
			}

			Effect.shake(shake, shake, this);
			receiveEffect.at(bullet.x, bullet.y, 0f, data.liquid.color);

			reloadCounter = 1f;
			//removing it makes it despawn, which spills whatever did not fit
			bullet.remove();
		}

		protected boolean shooterValid(LiquidMassDriverBuild other){
			return other.block == block && other.team == team && other.link == pos() && within(other, range);
		}

		/** The driver this one is linked to, if it is a valid target. */
		public LiquidMassDriverBuild linkedDriver(){
			if(link == -1) return null;
			Building other = world.build(link);
			if(!(other instanceof LiquidMassDriverBuild)) return null;
			LiquidMassDriverBuild d = (LiquidMassDriverBuild)other;
			return d.block == block && d.team == team && within(d, range) ? d : null;
		}

		protected boolean linkValid(){
			return linkedDriver() != null;
		}

		@Override
		public Point2 config(){
			if(tile == null) return null;
			return Point2.unpack(link).sub(tile.x, tile.y);
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.i(link);
			write.f(rotation);
			write.b((byte)state.ordinal());
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			link = read.i();
			rotation = read.f();
			state = DriverState.all[read.b()];
		}
	}
}
