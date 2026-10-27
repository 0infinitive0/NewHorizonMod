package newhorizon.expand.block.cargo;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.Stat;
import newhorizon.expand.hiemara.CargoCap;
import newhorizon.expand.hiemara.Hold;
import newhorizon.expand.units.unitEntity.TankerEntity;

/**
 * A block that cargo units are bound to: the item port and the liquid port. A unit is bound to one hub at a time and
 * only ever takes cargo from it. The binding dialog is the same for both.
 */
public abstract class HubBlock extends Block{
	/** Every hub block, filled by the constructor. */
	public static final Seq<HubBlock> all = new Seq<>();
	
	/** How many units can be bound to one hub. */
	public int maxBound = 4;
	
	protected final int timerPrune = timers++;
	
	public HubBlock(String name){
		super(name);
		
		update = true;
		solid = true;
		configurable = true;
		placeableLiquid = true;
		
		//Positive is a unit to bind, negative a unit to unbind.
		config(Integer.class, (HubBuild build, Integer id) -> {
			if(id > 0)build.bind(id);else build.unbind(-id);
		});
		
		//A unit to recycle.
		config(Long.class, (HubBuild build, Long id) -> build.recycle(id.intValue()));
		
		all.add(this);
	}
	
	/** @return what recycling a unit of this type gives back: part of the cost of the plan it is built by. */
	public static ItemStack[] refund(UnitType type){
		for(Block block : Vars.content.blocks()){
			if(!(block instanceof CargoUnitPad))continue;
			for(UnitFactory.UnitPlan plan : ((CargoUnitPad)block).plans){
				if(plan.unit != type)continue;
				ItemStack[] out = new ItemStack[plan.requirements.length];
				for(int i = 0; i < out.length; i++){
					ItemStack stack = plan.requirements[i];
					out[i] = new ItemStack(stack.item, Math.round(stack.amount * Vars.state.rules.deconstructRefundMultiplier));
				}
				return out;
			}
		}
		return new ItemStack[0];
	}
	
	/** @return whether units of this type can be bound to this kind of hub. */
	public abstract boolean accepts(UnitType type);
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.maxUnits, maxBound);
	}
	
	/** @return the hub a unit is bound to, or null. */
	public static HubBuild boundHub(Unit unit){
		for(HubBlock hub : all){
			for(Building build : unit.team.data().getBuildings(hub)){
				if(((HubBuild)build).bound.contains(unit.id))return (HubBuild)build;
			}
		}
		return null;
	}
	
	/** A short text on what a unit carries. */
	public static String loadText(Unit unit){
		if(unit instanceof TankerEntity){
			TankerEntity tanker = (TankerEntity)unit;
			return tanker.liquid == null || tanker.liquidAmount < 1f ? "" : Strings.fixed(tanker.liquidAmount, 0) + " " + tanker.liquid.localizedName;
		}
		if(Hold.multi(unit)){
			int total = Hold.total(unit);
			return total > 0 ? total + " (" + Hold.items(unit).size + ")" : "";
		}
		return unit.stack.amount > 0 ? unit.stack.amount + " " + unit.stack.item.localizedName : "";
	}
	
	public class HubBuild extends Building{
		public final IntSeq bound = new IntSeq();
		
		public boolean full(){
			return bound.size >= maxBound;
		}
		
		public void bind(int id){
			Unit unit = Groups.unit.getByID(id);
			if(unit == null || unit.team != team || !accepts(unit.type) || full() || bound.contains(id))return;
			if(boundHub(unit) != null)return;
			bound.add(id);
		}
		
		public void unbind(int id){
			bound.removeValue(id);
		}
		
		/** Removes a cargo unit and gives part of what it cost, and what it carries, to the core. */
		public void recycle(int id){
			Unit unit = Groups.unit.getByID(id);
			if(unit == null || unit.team != team || !accepts(unit.type) || unit.dead || !unit.isAdded())return;
			
			unbind(id);
			if(Vars.net.client())return;
			
			CoreBlock.CoreBuild core = team.core();
			if(core != null){
				for(ItemStack stack : refund(unit.type))give(core, stack.item, stack.amount);
				if(Hold.multi(unit)){
					for(Item item : Hold.items(unit))give(core, item, Hold.get(unit, item));
				}else if(unit.stack.amount > 0){
					give(core, unit.stack.item, unit.stack.amount);
				}
			}
			Call.unitDespawn(unit);
		}
		
		protected void give(CoreBlock.CoreBuild core, Item item, int amount){
			int room = core.storageCapacity - core.items.get(item);
			if(amount > 0 && room > 0)core.items.add(item, Math.min(amount, room));
		}
		
		/** Forgets units that are gone. */
		public void prune(){
			for(int i = bound.size - 1; i >= 0; i--){
				Unit unit = Groups.unit.getByID(bound.get(i));
				if(unit == null || !unit.isAdded() || unit.dead)bound.removeIndex(i);
			}
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
					}, true, () -> askRecycle(unit, rebuild[0]));
				}
				if(!any)content.add("@port.none-bound").color(Color.lightGray).left().row();
				
				content.add(Core.bundle.get("port.available")).color(Pal.accent).left().padTop(12f).row();
				content.image().color(Pal.accent).height(3f).growX().pad(4f).row();
				
				any = false;
				for(Unit unit : team.data().units){
					if(!accepts(unit.type) || boundHub(unit) != null)continue;
					any = true;
					unitRow(content, unit, "@port.bind", () -> {
						configure(unit.id);
						rebuild[0].run();
					}, !full(), () -> askRecycle(unit, rebuild[0]));
				}
				if(!any)content.add("@port.none-free").color(Color.lightGray).left().row();
			};
			rebuild[0].run();
			
			float[] elapsed = {0f};
			dialog.update(() -> {
				if(tile.build != this){
					dialog.hide();
					return;
				}
				elapsed[0] += Time.delta;
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
		
		protected void askRecycle(Unit unit, Runnable rebuild){
			StringBuilder refund = new StringBuilder();
			for(ItemStack stack : refund(unit.type)){
				if(stack.amount > 0)refund.append(stack.amount).append(' ').append(stack.item.localizedName).append(", ");
			}
			String text = refund.length() > 0 ? refund.substring(0, refund.length() - 2) : Core.bundle.get("port.recycle.nothing");
			Vars.ui.showConfirm("@port.recycle", Core.bundle.format("port.recycle.confirm", unit.type.localizedName + " #" + unit.id, text), () -> {
				configure(Long.valueOf(unit.id));
				rebuild.run();
			});
		}
		
		protected void unitRow(Table table, Unit unit, String label, Runnable action, boolean enabled, Runnable recycle){
			table.table(Styles.grayPanel, row -> {
				row.image(unit.type.uiIcon).size(40f).scaling(Scaling.fit).pad(6f);
				row.add(unit.type.localizedName + " #" + unit.id).left().growX();
				row.add(loadText(unit)).color(Color.lightGray).padRight(8f);
				row.button(label, Styles.defaultt, action).size(110f, 40f).disabled(b -> !enabled).pad(4f);
				row.button("@port.recycle", Styles.defaultt, recycle).size(110f, 40f).pad(4f);
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
