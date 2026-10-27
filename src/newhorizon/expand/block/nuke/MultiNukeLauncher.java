package newhorizon.expand.block.nuke;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.units.nuke.NukeBrain;
import newhorizon.expand.units.nuke.NukeDefense;

/**
 * A Nuke Launcher with a magazine: it holds several inactive nukes side by side, arms them all together and, when they are armed and
 * facing the right way, launches them one after the other in a quick salvo along the same heading, each at the map target if there is
 * one. Everything else works like the single launcher: the same menu, the same map target, and a pilot overrides the target, holds fire
 * to arm and launch, and lets go to disarm (until the salvo has started: once the first nuke is away, the rest follow).
 *
 * <p>The magazine starts with {@link #slots} slots; each launcher can be upgraded from its menu, a slot at a time, for {@link #upgradeCosts}
 * out of the core, up to {@link #maxSlots}. The nukes sit in a grid, {@link #columns} across.
 */
public class MultiNukeLauncher extends NukeLauncher{
	/** How many nukes fit in a new launcher, and the most an upgraded one can hold. */
	public int slots = 4, maxSlots = 20;
	/** What each upgrade costs, taken from the core: entry 0 buys the first extra slot, and each tier asks for more than the one before. */
	public ItemStack[][] upgradeCosts = {};
	/** Nukes side by side in the magazine, and the distance between neighbours across and along the heading. */
	public int columns = 5;
	public float slotSpacing = 8.5f, rowSpacing = 10f;
	/** Ticks between two nukes of a salvo. */
	public float salvoDelay = 8f;

	public MultiNukeLauncher(String name){
		super(name);
		size = 6;
		buildType = MultiNukeLauncherBuild::new;

		//The same commands as the single launcher, and 2: add a slot.
		config(Integer.class, (MultiNukeLauncherBuild b, Integer command) -> {
			if(command == 0) b.launch();
			else if(command == 1){
				b.hasTarget = false;
				b.launchOrder = false;
			}else if(command == 2) b.upgrade();
			else if(command == 3) b.simultaneous = !b.simultaneous;
		});
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-magazine", (MultiNukeLauncherBuild build) -> new Bar(
			() -> Core.bundle.format("nh.nuke.bar.magazine", build.stored.size, build.capacity),
			() -> Pal.accent,
			() -> build.stored.size / (float)build.capacity
		));
	}

	public class MultiNukeLauncherBuild extends NukeLauncherBuild{
		/**
		 * The nukes in the magazine, in slot order. The base class's single payload is only the nuke that is on its way in; once it has
		 * arrived it joins this list.
		 */
		public Seq<UnitPayload> stored = new Seq<>();
		/** Whether a salvo is under way, and the ticks until its next nuke. */
		public boolean firing;
		public float salvoTimer;
		/** The firing mode: false is a salvo, one nuke after another; true is every nuke in the same moment. */
		public boolean simultaneous;
		/** How many nukes this launcher can hold now. */
		public int capacity = slots;

		@Override
		public Unit loadedUnit(){
			return stored.isEmpty() ? null : stored.first().unit;
		}

		@Override
		public boolean loaded(){
			return !stored.isEmpty();
		}

		@Override
		public boolean isArmed(){
			return loaded() && stored.allMatch(p -> NukeUnits.isActive(p.unit.type));
		}

		/** Nukes are taken in while there is room, and only while they are not armed or arming: the whole magazine arms together. */
		protected boolean room(Unit unit){
			return payload == null && stored.size < capacity && !firing && armProgress <= 0f && !isArmed()
				&& NukeUnits.isInactive(unit.type);
		}

		@Override
		public boolean acceptUnitPayload(Unit unit){
			return room(unit) && unit.type.allowedInPayloads && !unit.spawnedByCore;
		}

		@Override
		public boolean acceptPayload(Building source, Payload p){
			return p instanceof UnitPayload && room(((UnitPayload)p).unit) && super.acceptPayload(source, p);
		}

		public int columnsNow(){
			return Math.min(capacity, columns);
		}

		/** The sideways offset of a slot from the centre line of the heading. */
		public float lateral(int slot){
			return (slot % columnsNow() - (columnsNow() - 1) / 2f) * slotSpacing;
		}

		/** How far a slot is along the heading from the middle: the first row is in front. */
		public float longitudinal(int slot){
			int rows = (capacity + columnsNow() - 1) / columnsNow();
			return ((rows - 1) / 2f - slot / columnsNow()) * rowSpacing;
		}

		/** Whether the core has enough of one of the upgrade's items. */
		protected boolean affords(ItemStack stack){
			return Vars.state.rules.infiniteResources || (team.core() != null && team.core().items.has(stack.item, stack.amount));
		}

		/** Whether there is another tier to buy. */
		public boolean upgradable(){
			return capacity < maxSlots && capacity - slots < upgradeCosts.length;
		}

		/** The price of the next slot. */
		public ItemStack[] nextCost(){
			return upgradeCosts[capacity - slots];
		}

		public boolean canUpgrade(){
			if(!upgradable()) return false;
			if(Vars.state.rules.infiniteResources) return true;
			return team.core() != null && team.core().items.has(nextCost());
		}

		/** Adds a slot and takes its price from the core. Everyone runs it, so everyone takes the price from their copy of the core. */
		public void upgrade(){
			if(!canUpgrade()) return;
			if(!Vars.state.rules.infiniteResources) team.core().items.remove(nextCost());
			capacity++;
		}

		protected void place(){
			for(int i = 0; i < stored.size; i++){
				float side = lateral(i), along = longitudinal(i);
				stored.get(i).set(x + Angles.trnsx(angle + 90f, side) + Angles.trnsx(angle, along), y + Angles.trnsy(angle + 90f, side) + Angles.trnsy(angle, along), angle);
			}
		}

		@Override
		public void updateTile(){
			unit();
			cooldown = Math.max(0f, cooldown - Time.delta);
			NukeBrain.driveLauncher(this);
			//A cloak put up over the target after it was picked takes it away.
			if(hasTarget && NukeDefense.cloaked(team, target.x, target.y)){
				hasTarget = false;
				launchOrder = false;
			}

			//The nuke on its way in joins the magazine when it gets to the middle.
			if(payload != null){
				boolean arrived = moveInPayload(false);
				payload.set(x + payVector.x, y + payVector.y, payRotation);
				if(arrived){
					stored.add(payload);
					payload = null;
					payVector.setZero();
				}
			}

			if(stored.isEmpty()){
				armProgress = 0f;
				wantArmed = false;
				armSwitch = false;
				launchOrder = false;
				firing = false;
				return;
			}
			if(efficiency <= 0.5f){
				place();
				return;
			}

			//Turn like a turret: to the pilot's cursor while someone controls it, otherwise to the map target, otherwise stay.
			wanted = angle;
			if(piloted()) wanted = angleTo(unit().aimX(), unit().aimY());
			else if(hasTarget) wanted = angleTo(target.x, target.y);
			angle = Angles.moveToward(angle, wanted, turnSpeed * edelta());
			place();

			//Once the salvo has started the nukes are armed for good; nothing arms or disarms until it is over.
			if(firing){
				salvo();
				return;
			}

			wantArmed = piloted() ? unit().isShooting() : armSwitch;
			float before = armProgress;
			armProgress = Mathf.approach(armProgress, wantArmed ? 1f : 0f, edelta() / armTime);
			if(armProgress >= 1f && !isArmed()) convert();
			else if(armProgress <= 0f && isArmed()) convert();
			if(armProgress > before && Mathf.chanceDelta(0.2)) NukeFx.armed.at(x, y);

			if(wantArmed && canLaunch() && autoLaunch()) launch();
		}

		@Override
		public int count(){
			return stored.size;
		}

		@Override
		public int capacityNow(){
			return capacity;
		}

		@Override
		public boolean salvoing(){
			return firing;
		}

		/** Swaps every nuke in the magazine for its other form. */
		@Override
		protected void convert(){
			for(UnitPayload p : stored){
				UnitType other = NukeUnits.counterpart(p.unit.type);
				if(other == null) continue;

				Unit now = other.create(team);
				now.rotation = p.unit.rotation;
				NukeUnits.transfer(p.unit, now);
				p.unit = now;
			}
			place();
		}

		@Override
		public boolean canLaunch(){
			return loaded() && isArmed() && payload == null && !firing && efficiency > 0.5f && cooldown <= 0f
				&& aimed() && Units.canCreate(team, loadedUnit().type);
		}

		/** Starts the salvo. */
		@Override
		public boolean launch(){
			if(!canLaunch()) return false;
			firing = true;
			salvoTimer = 0f;
			return true;
		}

		/** Sends the next nuke of the salvo when it is due and the heading is right; in the all-at-once mode every nuke goes together. */
		protected void salvo(){
			salvoTimer -= Time.delta;
			if(salvoTimer > 0f) return;
			if(stored.isEmpty()){
				endSalvo();
				return;
			}
			if(!aimed()) return;

			//The one in the last slot goes first, so the rest stay put.
			do{
				if(!Units.canCreate(team, stored.peek().unit.type)){
					//No room for more units: the rest stay armed, ready for when there is.
					firing = false;
					return;
				}
				fireOne();
			}while(simultaneous && !stored.isEmpty());

			if(stored.isEmpty()) endSalvo();
		}

		protected void fireOne(){
			int slot = stored.size - 1;
			Unit nuke = stored.pop().unit;
			//The pilot's heading overrides the map target: a nuke launched by hand just flies the way it was pointed.
			Vec2 aim = !piloted() && hasTarget ? new Vec2(target) : null;
			salvoTimer = salvoDelay;

			NukeFx.launch.at(x, y, angle);
			Sounds.shootMissileLarge.at(x, y, 1f, 0.8f);
			if(!Vars.net.client()) send(nuke, lateral(slot), aim);
		}

		protected void endSalvo(){
			firing = false;
			cooldown = launchCooldown;
			armProgress = 0f;
			wantArmed = false;
			armSwitch = false;
			launchOrder = false;
		}

		@Override
		public void onRemoved(){
			super.onRemoved();
			if(!carried){
				for(UnitPayload p : stored) p.dump();
				stored.clear();
			}
		}

		@Override
		public void buildConfiguration(Table table){
			super.buildConfiguration(table);
			table.row();
			table.table(Styles.black6, t -> {
				t.margin(8f);
				//Only the next step: the slot count it leads to, and what that costs (in red where the core is short).
				int[] shown = {-1};
				t.table(c -> {}).update(c -> {
					if(shown[0] == capacity) return;
					shown[0] = capacity;
					c.clear();
					if(!upgradable()){
						c.add(Core.bundle.format("nh.nuke.upgrade.max", capacity));
						return;
					}
					c.add(Core.bundle.format("nh.nuke.upgrade.info", capacity, capacity + 1)).padRight(10f);
					for(ItemStack stack : nextCost()){
						c.image(stack.item.uiIcon).size(24f);
						c.label(() -> (affords(stack) ? "" : "[scarlet]") + stack.amount).padLeft(3f).padRight(8f);
					}
				}).left().padBottom(6f).row();
				t.button(Core.bundle.get("nh.nuke.upgrade"), Styles.defaultt, () -> configure(2)).size(256f, 40f).disabled(b -> !canUpgrade());
			});
			table.row();
			//Salvo, or everything at once.
			table.table(Styles.black6, t -> {
				t.margin(8f);
				t.button(Core.bundle.get("nh.nuke.together"), Styles.togglet, () -> configure(3)).update(b -> b.setChecked(simultaneous)).size(256f, 40f);
			});
		}

		@Override
		public String statusText(){
			if(piloted()) return Core.bundle.get("nh.nuke.piloted");
			if(!loaded()) return Core.bundle.get("nh.nuke.empty");
			UnitType first = loadedUnit().type;
			String name = stored.allMatch(p -> p.unit.type == first) ? stored.size + "x " + first.localizedName : Core.bundle.format("nh.nuke.mixed", stored.size);
			if(efficiency <= 0.5f) return Core.bundle.format("nh.nuke.nopower", name);
			if(firing) return Core.bundle.format("nh.nuke.salvo", name);
			if(isArmed()) return Core.bundle.format(canLaunch() ? "nh.nuke.ready" : "nh.nuke.armed", name);
			if(wantArmed) return Core.bundle.format("nh.nuke.arming", name, (int)(armProgress * 100f));
			return Core.bundle.format("nh.nuke.loaded", name);
		}

		@Override
		public void draw(){
			Draw.rect(region, x, y);
			drawPayload();

			//An empty cradle for every slot there is room for.
			Draw.z(Layer.blockOver - 0.1f);
			Draw.color(Pal.darkestGray, 0.7f);
			for(int i = stored.size; i < capacity; i++){
				float side = lateral(i), along = longitudinal(i);
				Draw.rect(Core.atlas.white(), x + Angles.trnsx(angle + 90f, side) + Angles.trnsx(angle, along), y + Angles.trnsy(angle + 90f, side) + Angles.trnsy(angle, along), slotSpacing - 1.5f, rowSpacing - 1.5f, angle - 90f);
			}
			Draw.color();

			//The ring turns with the heading, under the nukes so that the ones in the corners are not covered by it.
			Draw.z(Layer.blockOver - 0.05f);
			Draw.rect(topRegion, x, y, angle - 90f);

			Draw.z(Layer.blockOver);
			for(UnitPayload p : stored) p.draw();

			if(loaded()){
				Draw.z(Layer.blockOver + 0.2f);
				float f = isArmed() ? 1f : armProgress;
				Draw.color(Color.scarlet, 0.35f + 0.65f * Mathf.absin(Time.time, 18f / (0.4f + f), 1f) * f);
				Draw.rect(topRegion, x, y, angle - 90f);
				Draw.color();
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.i(stored.size);
			for(UnitPayload p : stored) Payload.write(p, write);
			write.bool(firing);
			write.f(salvoTimer);
			write.i(capacity);
			write.bool(simultaneous);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			stored.clear();
			int n = read.i();
			for(int i = 0; i < n; i++){
				Payload p = Payload.read(read);
				if(p instanceof UnitPayload) stored.add((UnitPayload)p);
			}
			firing = read.bool();
			salvoTimer = read.f();
			capacity = Math.max(slots, Math.min(maxSlots, read.i()));
			simultaneous = read.bool();
		}
	}
}
