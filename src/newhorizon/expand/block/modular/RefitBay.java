package newhorizon.expand.block.modular;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.ui.Styles;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.ModularUnits;
import newhorizon.expand.units.modular.ModularAbility;
import newhorizon.expand.units.modular.ModulePart;

import static mindustry.Vars.tilesize;

/**
 * Where a modular frame is fitted out. It takes in a frame (as a payload, from a Foundry or a payload conveyor, or flown in by the
 * Enter Payload command) and holds it, and it takes in pods, which it keeps as a stock per kind of part. From its menu:
 * <ul>
 * <li>a part is installed into a slot of the frame from the stock; the part that was in the slot goes back to the stock, so installing is
 * also how a part is swapped;</li>
 * <li>a slot is emptied, its part going back to the stock;</li>
 * <li>a pod is let out of the stock, as a unit that flies off and fights;</li>
 * <li>the frame is let out, the way the bay faces.</li>
 * </ul>
 * <p>It can also run by itself: a preset says which part each slot should carry (or "leave it"), and with Auto on, a frame that arrives is
 * fitted from the stock one slot at a time until it matches the preset, then sent on out of the front. A frame that cannot be finished because a
 * part is out of stock waits for it, and says which. With a Foundry feeding it pods and frames, that is a line that turns out finished frames.
 *
 * <p>Changes need power. All of it is a config command (see {@link RefitBayBuild#command}), so every peer does the same.
 */
public class RefitBay extends PayloadBlock{
	/** How many pods of one kind the bay keeps. */
	public int stockCap = 20;

	/** Config commands: the kind of command times 1000 plus the slot times 100 plus the part. */
	public static final int installOp = 1, removeOp = 2, podOp = 3, frameOp = 4, presetOp = 5, presetClearOp = 6, presetCopyOp = 7, autoOp = 8;
	/** Ticks between one fitting and the next when running by itself. */
	public float autoInterval = 15f;

	public RefitBay(String name){
		super(name);
		size = 5;
		update = true;
		solid = true;
		hasPower = true;
		rotate = true;
		acceptsPayload = true;
		outputsPayload = true;
		configurable = true;
		group = BlockGroup.units;
		envEnabled = Env.any;
		consumePower(6f);

		config(Integer.class, (RefitBayBuild b, Integer code) -> b.command(code));
	}

	public static int code(int op, int slot, int part){
		return op * 1000 + slot * 100 + part;
	}

	public class RefitBayBuild extends PayloadBlockBuild<UnitPayload>{
		/** How many pods of each kind of part are in stock. */
		public int[] stock = new int[ModulePart.all.size];
		/** Whether the frame is leaving. */
		public boolean releasing;
		/** A flash after a fitting, for the looks. */
		public float flash;
		/** The part wanted in each slot (a part id), or -1 to leave the slot as it is. */
		public int[] preset = {-1, -1, -1, -1};
		/** Whether the bay fits a frame to the preset and sends it on by itself. */
		public boolean auto;
		/** The part an automatic fitting is waiting for, out of stock, or -1. */
		public int missing = -1;
		protected float autoTimer;

		public Unit frameUnit(){
			return payload == null ? null : payload.unit;
		}

		public ModularAbility fit(){
			return ModularAbility.of(frameUnit());
		}

		protected boolean isFrame(Unit unit){
			return unit.type == ModularUnits.frame;
		}

		@Override
		public boolean acceptUnitPayload(Unit unit){
			if(unit.team != team || unit.spawnedByCore || !unit.type.allowedInPayloads) return false;
			ModulePart part = ModulePart.ofPod(unit.type);
			return part != null ? stock[part.id] < stockCap : isFrame(unit) && payload == null;
		}

		@Override
		public boolean acceptPayload(Building source, Payload p){
			if(!(p instanceof UnitPayload)) return false;
			Unit unit = ((UnitPayload)p).unit;
			if(unit.team != team) return false;
			ModulePart part = ModulePart.ofPod(unit.type);
			return part != null ? stock[part.id] < stockCap : isFrame(unit) && payload == null;
		}

		@Override
		public void handlePayload(Building source, Payload p){
			Unit unit = ((UnitPayload)p).unit;
			ModulePart part = ModulePart.ofPod(unit.type);
			if(part != null){
				//A pod is taken apart into the stock.
				stock[part.id]++;
				flash = 1f;
				return;
			}
			super.handlePayload(source, p);
			releasing = false;
			ModularAbility fit = ModularAbility.of(unit);
			if(fit != null) fit.restore(unit);
		}

		@Override
		public void updateTile(){
			flash = Math.max(0f, flash - Time.delta / 40f);
			if(payload == null){
				releasing = false;
				return;
			}

			if(releasing){
				moveOutPayload();
				if(payload != null) payload.set(x + payVector.x, y + payVector.y, payRotation);
				return;
			}
			moveInPayload(false);
			if(payload != null) payload.set(x + payVector.x, y + payVector.y, payRotation);
			if(auto) autoStep();
		}

		/** Whether the bay may change anything now. */
		public boolean ready(){
			return efficiency > 0.5f && payload != null && payVector.isZero(0.5f) && !releasing;
		}

		/** Runs one command; the same on every peer. */
		public void command(int code){
			int op = code / 1000, slot = code / 100 % 10, part = code % 100;
			Unit unit = frameUnit();
			ModularAbility fit = fit();
			if(op == installOp){
				ModulePart p = ModulePart.get(part);
				if(unit == null || fit == null || p == null || !p.fits(slot) || stock[p.id] <= 0 || !ready()) return;
				install(unit, fit, slot, p);
			}else if(op == removeOp){
				if(unit == null || fit == null || !ready()) return;
				ModulePart old = fit.remove(unit, slot);
				if(old != null){
					stock[old.id] = Math.min(stockCap, stock[old.id] + 1);
					fitted();
				}
			}else if(op == presetOp){
				ModulePart p = ModulePart.get(part);
				if(p != null && p.fits(slot)) preset[slot] = p.id;
			}else if(op == presetClearOp){
				if(slot >= 0 && slot < preset.length) preset[slot] = -1;
			}else if(op == presetCopyOp){
				if(fit != null) System.arraycopy(fit.installed, 0, preset, 0, preset.length);
			}else if(op == autoOp){
				auto = part != 0;
				autoTimer = 0f;
				missing = -1;
			}else if(op == podOp){
				ModulePart p = ModulePart.get(part);
				if(p == null || p.pod == null || stock[p.id] <= 0 || !Units.canCreate(team, p.pod)) return;
				stock[p.id]--;
				flash = 1f;
				if(Vars.net.client()) return;
				Unit pod = p.pod.create(team);
				float angle = rotdeg();
				pod.set(x + Mathf.cosDeg(angle) * (size * tilesize / 2f + 8f), y + Mathf.sinDeg(angle) * (size * tilesize / 2f + 8f));
				pod.rotation = angle;
				pod.add();
				Fx.spawn.at(pod);
			}else if(op == frameOp){
				if(payload != null && payVector.isZero(0.5f)) releasing = true;
			}
		}

		/** Puts a part from the stock into a slot; the part that was there goes back to the stock. */
		protected void install(Unit unit, ModularAbility fit, int slot, ModulePart p){
			stock[p.id]--;
			ModulePart old = fit.install(unit, slot, p);
			if(old != null) stock[old.id] = Math.min(stockCap, stock[old.id] + 1);
			fitted();
		}

		/**
		 * One step of the bay running by itself: fits the first slot that does not match the preset, from the stock, and when every slot does,
		 * sends the frame on. A slot whose part is out of stock holds the frame back, and {@link #missing} says which part.
		 */
		protected void autoStep(){
			if(!ready()) return;
			Unit unit = frameUnit();
			ModularAbility fit = fit();
			if(unit == null || fit == null) return;
			autoTimer += Time.delta;
			if(autoTimer < autoInterval) return;
			autoTimer = 0f;

			missing = -1;
			for(int slot = 0; slot < preset.length; slot++){
				ModulePart want = ModulePart.get(preset[slot]);
				if(want == null || !want.fits(slot) || fit.installed[slot] == want.id) continue;
				if(stock[want.id] > 0){
					install(unit, fit, slot, want);
					return;
				}
				missing = want.id;
			}
			if(missing < 0) releasing = true;
		}

		/** The command that moves a slot's preset on to the next part that fits it, and after the last one to "leave it". */
		public int nextPreset(int slot){
			arc.struct.Seq<ModulePart> fits = ModulePart.all.select(p -> p.fits(slot));
			int at = -1;
			for(int i = 0; i < fits.size; i++){
				if(fits.get(i).id == preset[slot]) at = i;
			}
			int next = at + 1;
			return next >= fits.size ? code(presetClearOp, slot, 0) : code(presetOp, slot, fits.get(next).id);
		}

		public String presetName(int slot){
			ModulePart p = ModulePart.get(preset[slot]);
			return p == null ? Core.bundle.get("nh.modular.leave") : p.localized();
		}

		protected void fitted(){
			flash = 1f;
			Sounds.click.at(x, y, 1.2f);
			Fx.spawn.at(x, y);
		}

		@Override
		public void buildConfiguration(Table table){
			table.pane(Styles.noBarPane, scroll -> scroll.table(Styles.black6, t -> {
				t.margin(8f);
				t.defaults().left().padBottom(4f);
				t.label(() -> statusText()).width(300f).wrap().row();

				//The preset and the Auto switch: a frame that comes in is fitted to it and sent on by itself.
				t.add(Core.bundle.get("nh.modular.preset")).row();
				String[] presetNames = {"nh.modular.armleft", "nh.modular.armright", "nh.modular.core", "nh.modular.hull"};
				for(int slot = 0; slot < ModulePart.slots; slot++){
					final int s = slot;
					t.button("", Styles.defaultt, () -> configure(nextPreset(s))).size(300f, 34f).update(b -> b.setText(Core.bundle.get(presetNames[s]) + ": " + presetName(s))).row();
				}
				t.table(row -> {
					row.defaults().size(148f, 36f).pad(2f);
					row.button(Core.bundle.get("nh.modular.copy"), Styles.defaultt, () -> configure(code(presetCopyOp, 0, 0))).disabled(b -> fit() == null);
					row.button("", Styles.togglet, () -> configure(code(autoOp, 0, auto ? 0 : 1))).update(b -> {
						b.setText(Core.bundle.get(auto ? "nh.modular.autoon" : "nh.modular.autooff"));
						b.setChecked(auto);
					});
				}).row();

				String[] slotNames = {"nh.modular.armleft", "nh.modular.armright", "nh.modular.core", "nh.modular.hull"};
				for(int slot = 0; slot < ModulePart.slots; slot++){
					final int s = slot;
					t.label(() -> Core.bundle.get(slotNames[s]) + ": " + (fit() == null || fit().part(s) == null ? Core.bundle.get("nh.modular.empty") : fit().part(s).localized())).row();
					t.table(row -> {
						row.defaults().size(96f, 40f).pad(2f);
						int shown = 0;
						for(ModulePart part : ModulePart.all){
							if(!part.fits(s)) continue;
							//Four to a row: an arm slot takes a dozen parts.
							if(shown++ > 0 && shown % 4 == 1) row.row();
							row.button(part.localized() + "\n[lightgray]x" + "[]", Styles.togglet, () -> configure(code(installOp, s, part.id)))
								.update(b -> {
									b.setText(part.localized() + "\n[lightgray]x" + stock[part.id]);
									b.setChecked(fit() != null && fit().installed[s] == part.id);
								})
								.disabled(b -> !ready() || stock[part.id] <= 0 || fit() == null);
						}
						row.button(Core.bundle.get("nh.modular.remove"), Styles.defaultt, () -> configure(code(removeOp, s, 0)))
							.disabled(b -> !ready() || fit() == null || fit().part(s) == null);
					}).row();
				}

				t.add(Core.bundle.get("nh.modular.pods")).row();
				t.table(row -> {
					row.defaults().size(96f, 36f).pad(2f);
					int n = 0;
					for(ModulePart part : ModulePart.all){
						row.button("", Styles.defaultt, () -> configure(code(podOp, 0, part.id)))
							.update(b -> b.setText(part.localized() + " x" + stock[part.id]))
							.disabled(b -> stock[part.id] <= 0);
						if(++n % 3 == 0) row.row();
					}
				}).row();

				t.button(Core.bundle.get("nh.modular.release"), Styles.defaultt, () -> {
					configure(code(frameOp, 0, 0));
					Vars.control.input.config.hideConfig();
				}).size(200f, 40f).disabled(b -> payload == null || !payVector.isZero(0.5f) || releasing);
			})).maxHeight(440f);
		}

		public String statusText(){
			if(payload == null) return Core.bundle.get("nh.modular.noframe");
			if(efficiency <= 0.5f) return Core.bundle.get("nh.modular.nopower");
			if(auto && missing >= 0) return Core.bundle.format("nh.modular.waiting", ModulePart.get(missing).localized());
			if(auto) return Core.bundle.get("nh.modular.auto");
			return Core.bundle.get("nh.modular.ready");
		}

		@Override
		public void draw(){
			Draw.rect(region, x, y);
			drawPayload();

			Draw.z(Layer.blockOver + 0.1f);
			Draw.rect(topRegion, x, y, rotdeg());
			if(flash > 0f){
				Draw.z(Layer.blockOver + 0.2f);
				Draw.color(Color.white, flash * 0.6f);
				Draw.rect(topRegion, x, y, rotdeg());
				Draw.color();
			}
		}

		@Override
		public byte version(){
			return 1;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.i(stock.length);
			for(int n : stock) write.i(n);
			write.i(preset.length);
			for(int n : preset) write.i(n);
			write.bool(auto);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			int n = read.i();
			for(int i = 0; i < n; i++){
				int v = read.i();
				if(i < stock.length) stock[i] = Mathf.clamp(v, 0, stockCap);
			}
			//Saves from before the preset have none.
			if(revision >= 1){
				int m = read.i();
				for(int i = 0; i < m; i++){
					int v = read.i();
					if(i < preset.length) preset[i] = ModulePart.get(v) != null && ModulePart.get(v).fits(i) ? v : -1;
				}
				auto = read.bool();
			}
		}
	}
}
