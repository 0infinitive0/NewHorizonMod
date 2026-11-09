package newhorizon.expand.block.supply;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.Sector;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import java.util.Arrays;

import static mindustry.Vars.state;

/**
 * Sends items straight into the core of another sector of the same planet (one the player holds a base on). It is filled by conveyors or
 * unloaders with the item types ticked in its configuration, and every {@link #sendTime} ticks of powered time it sends all it holds that
 * the destination's core has room for; the rest stays inside.
 *
 * <p>The delivery is the base game's own {@link Sector#addItems}: items go into the core if that sector is being played, or into the stored
 * items of the sector (applied to its core the next time it loads) if not. It is instant and nothing is lost. This is not a {@code LaunchPad}
 * (the base game's pads deliver nothing by themselves, and the mod bans them on some planets).
 *
 * <p>Config: an {@link IntSeq} of the target sector's id followed by the ids of the ticked items.
 */
public class SectorLauncher extends Block{
	/** Ticks of powered time between two launches. */
	public float sendTime = 60f * 10f;
	public Color color = Color.valueOf("7fd8ff");
	public TextureRegion topRegion;
	public Effect launchEffect = Fx.launchPod;

	public SectorLauncher(String name){
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		hasItems = true;
		unloadable = false;
		configurable = true;
		saveConfig = true;
		copyConfig = true;
		itemCapacity = 300;
		envEnabled = Env.any;
		buildType = SectorLauncherBuild::new;

		config(IntSeq.class, (SectorLauncherBuild b, IntSeq s) -> b.apply(s));
		configClear((SectorLauncherBuild b) -> b.apply(null));
	}

	@Override
	public void load(){
		super.load();
		topRegion = Core.atlas.find(name + "-top");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);
		stats.add(new Stat("nh-launcher-batch", mindustry.world.meta.StatCat.function), Core.bundle.format("stat.nh-launcher-batch.value", (int)(sendTime / 60f)));
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-launcher-time", (SectorLauncherBuild b) -> new Bar(
			() -> Core.bundle.format("bar.nh-launcher-time", (int)Math.max(0f, (sendTime - b.timer) / 60f)),
			() -> color,
			() -> b.timer / sendTime));
		addBar("nh-launcher-target", (SectorLauncherBuild b) -> new Bar(
			() -> b.targetName(),
			() -> Pal.accent,
			() -> b.validTarget(b.target()) ? 1f : 0f));
	}

	public class SectorLauncherBuild extends Building{
		/** The target sector's id on the current planet, or -1. */
		public int sector = -1;
		/** Which items it takes, by item id. */
		public boolean[] selected = new boolean[Vars.content.items().size];
		public float timer;
		/** How many items the last launch sent, for the display. */
		public int lastSent;

		/** Sets the sector and the items from a config (null clears it). */
		public void apply(IntSeq s){
			Arrays.fill(selected, false);
			sector = -1;
			if(s == null || s.size == 0) return;
			sector = s.get(0);
			for(int i = 1; i < s.size; i++){
				int id = s.get(i);
				if(id >= 0 && id < selected.length) selected[id] = true;
			}
		}

		@Override
		public Object config(){
			IntSeq s = new IntSeq();
			s.add(sector);
			for(int i = 0; i < selected.length; i++) if(selected[i]) s.add(i);
			return s;
		}

		public void toggle(Item item){
			IntSeq s = (IntSeq)config();
			int at = -1;
			for(int i = 1; i < s.size; i++) if(s.get(i) == item.id) at = i;
			if(at >= 0) s.removeIndex(at); else s.add(item.id);
			configure(s);
		}

		public Sector target(){
			Sector cur = state.rules.sector;
			if(cur == null || sector < 0 || sector >= cur.planet.sectors.size) return null;
			return cur.planet.sectors.get(sector);
		}

		/** Whether items can be sent to {@code dst}: another sector of the same planet with a base of ours. */
		protected boolean validTarget(Sector dst){
			Sector cur = state.rules.sector;
			return cur != null && dst != null && dst != cur && dst.planet == cur.planet && dst.hasBase();
		}

		/** How many more of an item the destination can take. */
		protected int room(Sector dst, Item item){
			return Math.max(0, dst.info.storageCapacity - dst.items().get(item));
		}

		/** Puts items into the destination. Overridable by a test. */
		protected void deliver(Sector dst, ItemSeq seq){
			dst.addItems(seq);
		}

		public String targetName(){
			Sector dst = target();
			if(dst == null) return Core.bundle.get("bar.nh-launcher-none");
			return Core.bundle.format("bar.nh-launcher-target", dst.name());
		}

		public boolean holdsAny(){
			for(int i = 0; i < selected.length; i++){
				if(selected[i] && items.get(Vars.content.item(i)) > 0) return true;
			}
			return false;
		}

		@Override
		public boolean acceptItem(Building source, Item item){
			return state.isCampaign() && selected[item.id] && items.get(item) < itemCapacity;
		}

		@Override
		public int getMaximumAccepted(Item item){
			return itemCapacity;
		}

		@Override
		public boolean shouldConsume(){
			return state.isCampaign() && holdsAny() && validTarget(target());
		}

		@Override
		public void updateTile(){
			if(Vars.net.client() || !state.isCampaign()) return;
			Sector dst = target();
			if(efficiency <= 0f || !holdsAny() || !validTarget(dst)) return;

			timer = Math.min(sendTime, timer + delta() * efficiency);
			if(timer >= sendTime){
				send(dst);
			}
		}

		/** Sends what fits. A launch that sent nothing (the destination is full) tries again in a second. */
		public void send(Sector dst){
			ItemSeq seq = new ItemSeq();
			int total = 0;
			for(int i = 0; i < selected.length; i++){
				if(!selected[i]) continue;
				Item item = Vars.content.item(i);
				int amount = Math.min(items.get(item), room(dst, item));
				if(amount > 0){
					seq.add(item, amount);
					total += amount;
				}
			}
			if(total == 0){
				timer = sendTime - 60f;
				return;
			}

			deliver(dst, seq);
			Sector src = state.rules.sector;
			seq.each((item, amount) -> {
				items.remove(item, amount);
				if(src != null) src.info.handleItemExport(item, amount);
			});
			lastSent = total;
			timer = 0f;
			launchEffect.at(x, y);
			Sounds.padLaunch.at(x, y, 1f + Mathf.range(0.1f));
			mindustry.entities.Effect.shake(2f, 2f, this);
		}

		@Override
		public boolean shouldShowConfigure(Player player){
			return state.isCampaign();
		}

		@Override
		public void buildConfiguration(Table table){
			if(!state.isCampaign() || Vars.net.client()){
				deselect();
				return;
			}
			table.defaults().pad(4f);
			table.table(top -> {
				top.button(Core.bundle.get("config.nh-launcher-pick"), Icon.upOpen, Styles.cleart, () -> {
					Vars.ui.planet.showSelect(state.rules.sector, other -> {
						if(state.rules.sector != null && other != state.rules.sector && other.planet == state.rules.sector.planet){
							IntSeq s = (IntSeq)config();
							s.set(0, other.id);
							configure(s);
						}
					});
					deselect();
				}).size(180f, 40f);
				top.label(() -> targetName()).padLeft(8f);
			}).row();

			table.table(all -> {
				all.button(Core.bundle.get("config.nh-launcher-all"), Styles.cleart, () -> {
					IntSeq s = new IntSeq();
					s.add(sector);
					for(Item item : Vars.content.items()) if(item.unlockedNow() && !item.isHidden()) s.add(item.id);
					configure(s);
				}).size(90f, 34f);
				all.button(Core.bundle.get("config.nh-launcher-none"), Styles.cleart, () -> {
					IntSeq s = new IntSeq();
					s.add(sector);
					configure(s);
				}).size(90f, 34f);
			}).row();

			table.table(grid -> {
				int i = 0;
				for(Item item : Vars.content.items()){
					if(!item.unlockedNow() || item.isHidden()) continue;
					grid.button(new TextureRegionDrawable(item.uiIcon), Styles.clearTogglei, 32f, () -> toggle(item))
						.size(42f).update(b -> b.setChecked(selected[item.id])).tooltip(item.localizedName);
					if(++i % 8 == 0) grid.row();
				}
			});
		}

		@Override
		public void draw(){
			super.draw();
			if(topRegion != null && topRegion.found()){
				Draw.z(Layer.block + 0.1f);
				float glow = holdsAny() && efficiency > 0f ? 0.5f + 0.5f * (timer / sendTime) : 0.25f;
				Draw.color(color, glow);
				Draw.rect(topRegion, x, y);
				Draw.reset();
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.i(sector);
			int n = 0;
			for(boolean b : selected) if(b) n++;
			write.s(n);
			for(int i = 0; i < selected.length; i++) if(selected[i]) write.s(i);
			write.f(timer);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision >= 1){
				sector = read.i();
				int n = read.s();
				Arrays.fill(selected, false);
				for(int i = 0; i < n; i++){
					int id = read.s();
					if(id >= 0 && id < selected.length) selected[id] = true;
				}
				timer = read.f();
			}
		}

		@Override
		public byte version(){
			return 1;
		}
	}
}
