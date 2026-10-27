package newhorizon.expand.block.cargo;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import newhorizon.expand.hiemara.CargoCap;

import static mindustry.Vars.tilesize;

/**
 * Where cargo units pick up what they carry. It holds any mix of items. A unit is bound to one port at a time and
 * only ever takes items from it.
 */
public class Port extends Block{
	/** Every port block, filled by the constructor. */
	public static final Seq<Port> all = new Seq<>();
	
	/** How many units can be bound to one port. */
	public int maxBound = 4;
	
	protected final int timerPrune = timers++;
	
	public Port(String name){
		super(name);
		
		update = true;
		solid = true;
		hasItems = true;
		configurable = true;
		placeableLiquid = true;
		itemCapacity = 300;
		
		//Positive is a unit to bind, negative a unit to unbind.
		config(Integer.class, (PortBuild build, Integer id) -> {
			if(id > 0)build.bind(id);else build.unbind(-id);
		});
		
		all.add(this);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.maxUnits, maxBound);
	}
	
	/** @return the port a unit is bound to, or null. */
	public static PortBuild boundPort(Unit unit){
		for(Port port : all){
			for(Building build : unit.team.data().getBuildings(port)){
				if(((PortBuild)build).bound.contains(unit.id))return (PortBuild)build;
			}
		}
		return null;
	}
	
	public class PortBuild extends Building{
		public final IntSeq bound = new IntSeq();
		
		public boolean full(){
			return bound.size >= maxBound;
		}
		
		public void bind(int id){
			Unit unit = Groups.unit.getByID(id);
			if(unit == null || unit.team != team || !CargoCap.isCargo(unit.type) || full() || bound.contains(id))return;
			if(boundPort(unit) != null)return;
			bound.add(id);
		}
		
		public void unbind(int id){
			bound.removeValue(id);
		}
		
		/** Forgets units that are gone. */
		public void prune(){
			for(int i = bound.size - 1; i >= 0; i--){
				Unit unit = Groups.unit.getByID(bound.get(i));
				if(unit == null || !unit.isAdded() || unit.dead)bound.removeIndex(i);
			}
		}
		
		@Override
		public boolean acceptItem(Building source, Item item){
			return items.total() < itemCapacity && !(source instanceof Depot.DepotBuild) && !(source instanceof PortBuild);
		}
		
		@Override
		public int acceptStack(Item item, int amount, mindustry.gen.Teamc source){
			return 0;
		}
		
		@Override
		public int getMaximumAccepted(Item item){
			return itemCapacity;
		}
		
		@Override
		public void updateTile(){
			if(timer(timerPrune, 60f))prune();
		}
		
		@Override
		public void buildConfiguration(Table table){
			table.button(Icon.units, Styles.cleari, this::openDialog).size(50f).tooltip("@port.manage");
		}
		
		protected void openDialog(){
			BaseDialog dialog = new BaseDialog("@port.manage");
			Table content = new Table();
			
			Runnable[] rebuild = {null};
			rebuild[0] = () -> {
				content.clear();
				content.top().left();
				
				content.add(Core.bundle.format("port.bound", bound.size, maxBound)).color(Pal.accent).left().row();
				content.image().color(Pal.accent).height(3f).growX().pad(4f).row();
				
				boolean any = false;
				for(int i = 0; i < bound.size; i++){
					Unit unit = Groups.unit.getByID(bound.get(i));
					if(unit == null)continue;
					any = true;
					unitRow(content, unit, "@port.unbind", () -> {
						configure(-unit.id);
						rebuild[0].run();
					}, true);
				}
				if(!any)content.add("@port.none-bound").color(arc.graphics.Color.lightGray).left().row();
				
				content.add(Core.bundle.get("port.available")).color(Pal.accent).left().padTop(12f).row();
				content.image().color(Pal.accent).height(3f).growX().pad(4f).row();
				
				any = false;
				for(Unit unit : team.data().units){
					if(!CargoCap.isCargo(unit.type) || boundPort(unit) != null)continue;
					any = true;
					unitRow(content, unit, "@port.bind", () -> {
						configure(unit.id);
						rebuild[0].run();
					}, !full());
				}
				if(!any)content.add("@port.none-free").color(arc.graphics.Color.lightGray).left().row();
			};
			rebuild[0].run();
			
			float[] elapsed = {0f};
			dialog.update(() -> {
				if(tile.build != this){
					dialog.hide();
					return;
				}
				elapsed[0] += arc.util.Time.delta;
				if(elapsed[0] > 30f){
					elapsed[0] = 0f;
					rebuild[0].run();
				}
			});
			
			dialog.cont.pane(content).grow().minWidth(420f).minHeight(300f);
			dialog.addCloseButton();
			dialog.show();
			deselect();
		}
		
		protected void unitRow(Table table, Unit unit, String label, Runnable action, boolean enabled){
			table.table(Styles.grayPanel, row -> {
				row.image(unit.type.uiIcon).size(40f).scaling(Scaling.fit).pad(6f);
				row.add(unit.type.localizedName + " #" + unit.id).left().growX();
				row.add(unit.stack.amount > 0 ? unit.stack.amount + " " + unit.stack.item.localizedName : "").color(arc.graphics.Color.lightGray).padRight(8f);
				row.button(label, Styles.defaultt, action).size(110f, 40f).disabled(b -> !enabled).pad(4f);
			}).growX().padBottom(4f).row();
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.i(bound.size);
			for(int i = 0; i < bound.size; i++)write.i(bound.get(i));
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			bound.clear();
			int size = read.i();
			for(int i = 0; i < size; i++)bound.add(read.i());
		}
	}
}
