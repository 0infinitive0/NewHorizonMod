package newhorizon.expand.block.cargo;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;

/**
 * A distributor and an improved port in one block. Every second it fills the inputs of the blocks around it from what it holds, and
 * collects what they have made. It can be set to hold an amount of each of a list of items: cargo units bring what is short, and
 * only take what is more (and every item that is not on the list), so the stock stays.
 */
public class SupplyHub extends Port{
	/** Every supply hub block, filled by the constructor. */
	public static final Seq<SupplyHub> hubs = new Seq<>();
	
	/** Reach in tiles. */
	public float range = 12f;
	/** Most of one item given to one block in one second. */
	public int fillPerSecond = 30;
	/** Most of one item taken from one block in one second. */
	public int collectPerItem = 40;
	/** How many kinds of item can be on the list. */
	public int maxTargets = 12;
	
	protected final int timerFill = timers++, timerCollect = timers++;
	
	public SupplyHub(String name){
		super(name);
		
		saveConfig = true;
		clearOnDoubleTap = true;
		
		//pairs of item id and amount to hold
		config(int[].class, (SupplyHubBuild build, int[] pairs) -> build.setTargets(pairs));
		configClear((SupplyHubBuild build) -> build.targets.clear());
		
		hubs.add(this);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.range, range, StatUnit.blocks);
		stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);
		stats.add(Stat.abilities, Core.bundle.format("stat.supply-hub", maxTargets));
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, Pal.accent);
	}
	
	public class SupplyHubBuild extends PortBuild implements Dropoff{
		/** The amount to hold of each listed item. */
		public final ObjectIntMap<Item> targets = new ObjectIntMap<>();
		
		public int target(Item item){
			return targets.get(item, 0);
		}
		
		/** Sets the list from pairs of item id and amount. The total is kept within the capacity. */
		public void setTargets(int[] pairs){
			targets.clear();
			int total = 0;
			for(int i = 0; i + 1 < pairs.length && targets.size < maxTargets; i += 2){
				Item item = Vars.content.item(pairs[i]);
				int amount = Math.max(0, pairs[i + 1]);
				if(item == null || amount <= 0 || targets.containsKey(item))continue;
				amount = Math.min(amount, itemCapacity - total);
				if(amount <= 0)break;
				targets.put(item, amount);
				total += amount;
			}
		}
		
		/** @return the list as pairs of item id and amount. */
		@Override
		public int[] config(){
			int[] pairs = new int[targets.size * 2];
			int i = 0;
			for(var entry : targets){
				pairs[i++] = entry.key.id;
				pairs[i++] = entry.value;
			}
			return pairs;
		}
		
		/** Cargo units only take what is more than the amount to hold. */
		@Override
		public int available(Item item){
			return Math.max(0, items.get(item) - target(item));
		}
		
		@Override
		public boolean wants(Item item){
			return items.get(item) < target(item) && items.total() < itemCapacity;
		}
		
		@Override
		public int room(){
			return Math.max(0, itemCapacity - items.total());
		}
		
		@Override
		public boolean nextToCore(){
			return false;
		}
		
		/** Cargo units bring the items that are short, no more than the amount to hold. */
		@Override
		public int acceptStack(Item item, int amount, Teamc source){
			if(!(source instanceof Unit) || !wants(item))return 0;
			return Math.min(Math.min(amount, target(item) - items.get(item)), itemCapacity - items.total());
		}
		
		@Override
		public void updateTile(){
			super.updateTile();
			
			if(Vars.net.client())return;
			if(timer(timerFill, 60f) && items.total() > 0)Distributor.fillAround(this, range, fillPerSecond);
			if(timer(timerCollect, 60f))ImprovedPort.collectAround(this, range, collectPerItem);
		}
		
		@Override
		public void buildConfiguration(Table table){
			super.buildConfiguration(table);
			table.button(Icon.list, Styles.cleari, this::openTargets).size(50f).tooltip("@supply.targets");
		}
		
		protected void openTargets(){
			BaseDialog dialog = new BaseDialog("@supply.targets");
			Table content = new Table();
			
			//the list being edited, applied on every change
			ObjectIntMap<Item> edit = new ObjectIntMap<>();
			edit.putAll(targets);
			
			Runnable[] rebuild = {null};
			Runnable apply = () -> {
				int[] pairs = new int[edit.size * 2];
				int i = 0;
				for(var entry : edit){
					pairs[i++] = entry.key.id;
					pairs[i++] = entry.value;
				}
				configure(pairs);
			};
			
			rebuild[0] = () -> {
				content.clear();
				content.top().left();
				content.add(Core.bundle.format("supply.hold", edit.size, maxTargets)).color(Pal.accent).left().row();
				content.image().color(Pal.accent).height(3f).growX().pad(4f).row();
				
				for(Item item : Vars.content.items().select(i -> edit.containsKey(i))){
					content.table(Styles.grayPanel, row -> {
						row.image(item.uiIcon).size(32f).scaling(Scaling.fit).pad(6f);
						row.add(item.localizedName).left().width(120f);
						
						var label = row.add(String.valueOf(edit.get(item, 0))).width(70f).color(Color.lightGray).get();
						Slider slider = new Slider(0f, itemCapacity, 10f, false);
						slider.setValue(edit.get(item, 0));
						slider.moved(v -> {
							edit.put(item, (int)v);
							label.setText(String.valueOf((int)v));
						});
						slider.released(() -> {
							if(edit.get(item, 0) <= 0)edit.remove(item, 0);
							apply.run();
							rebuild[0].run();
						});
						row.add(slider).width(220f).pad(6f);
						
						row.button(Icon.cancel, Styles.cleari, () -> {
							edit.remove(item, 0);
							apply.run();
							rebuild[0].run();
						}).size(40f).pad(4f);
					}).growX().padBottom(4f).row();
				}
				
				if(edit.size < maxTargets){
					content.add("@supply.add").color(Pal.accent).left().padTop(10f).row();
					content.table(t -> {
						int n = 0;
						for(Item item : Vars.content.items()){
							if(edit.containsKey(item) || item.hidden)continue;
							t.button(new arc.scene.style.TextureRegionDrawable(item.uiIcon), Styles.cleari, () -> {
								edit.put(item, Math.min(100, itemCapacity));
								apply.run();
								rebuild[0].run();
							}).size(44f).pad(2f).tooltip(item.localizedName);
							if(++n % 8 == 0)t.row();
						}
					}).left().row();
				}
			};
			rebuild[0].run();
			
			dialog.update(() -> {
				if(tile.build != this)dialog.hide();
			});
			dialog.cont.pane(content).grow().minWidth(560f).minHeight(300f);
			dialog.addCloseButton();
			dialog.show();
			deselect();
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range * tilesize, team.color);
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.i(targets.size);
			for(var entry : targets){
				write.s(entry.key.id);
				write.i(entry.value);
			}
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			targets.clear();
			int size = read.i();
			for(int i = 0; i < size; i++){
				Item item = Vars.content.item(read.s());
				int amount = read.i();
				if(item != null)targets.put(item, amount);
			}
		}
	}
}
