package newhorizon.expand.block.cargo;

import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.type.Item;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;

/**
 * Where cargo units drop what they carry. It can be set to a single item. Everything it holds is pushed on into the
 * blocks next to it, the core included.
 */
public class Depot extends Block{
	/** Every depot block, filled by the constructor. */
	public static final arc.struct.Seq<Depot> all = new arc.struct.Seq<>();
	
	public Depot(String name){
		super(name);
		
		update = true;
		solid = true;
		hasItems = true;
		configurable = true;
		saveConfig = true;
		clearOnDoubleTap = true;
		placeableLiquid = true;
		itemCapacity = 200;
		
		config(Item.class, (DepotBuild build, Item item) -> build.item = item);
		configClear((DepotBuild build) -> build.item = null);
		
		all.add(this);
	}
	
	public class DepotBuild extends Building{
		/** Only this item is accepted, any if null. */
		public Item item;
		
		/** @return whether the depot is set to take this item. */
		public boolean matches(Item item){
			return this.item == null || this.item == item;
		}
		
		/** @return whether a unit may bring this item. */
		public boolean wants(Item item){
			return matches(item) && items.total() < itemCapacity;
		}
		
		/** @return how many more items fit: a core next to the depot takes everything. */
		public int room(){
			int room = itemCapacity - items.total();
			for(Building other : proximity){
				if(other instanceof CoreBlock.CoreBuild)return Integer.MAX_VALUE / 2;
			}
			return Math.max(0, room);
		}
		
		@Override
		public boolean acceptItem(Building source, Item item){
			//Only cargo units fill a depot.
			return false;
		}
		
		@Override
		public int acceptStack(Item item, int amount, Teamc source){
			if(!(source instanceof mindustry.gen.Unit) || !matches(item))return 0;
			int room = itemCapacity - items.total();
			//a core next to the depot takes it all at once
			for(Building other : proximity){
				if(other instanceof CoreBlock.CoreBuild)room += other.acceptStack(item, amount, this);
			}
			return Math.min(room, amount);
		}
		
		@Override
		public void handleStack(Item item, int amount, Teamc source){
			for(Building other : proximity){
				if(amount <= 0)break;
				if(other instanceof CoreBlock.CoreBuild){
					int accepted = other.acceptStack(item, amount, this);
					if(accepted > 0){
						other.handleStack(item, accepted, this);
						amount -= accepted;
					}
				}
			}
			if(amount > 0)super.handleStack(item, amount, source);
		}
		
		@Override
		public boolean canDump(Building to, Item item){
			return !(to instanceof DepotBuild) && !(to instanceof Port.PortBuild);
		}
		
		@Override
		public void updateTile(){
			//Several items at a time, so a big load does not take long to move on.
			for(int i = 0; i < 6 && items.total() > 0; i++){
				if(!dump())break;
			}
		}
		
		@Override
		public void buildConfiguration(Table table){
			ItemSelection.buildTable(Depot.this, table, Vars.content.items(), () -> item, this::configure);
		}
		
		@Override
		public Item config(){
			return item;
		}
		
		@Override
		public void drawSelect(){
			drawItemSelection(item);
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.s(item == null ? -1 : item.id);
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			int id = read.s();
			item = id == -1 ? null : Vars.content.item(id);
		}
	}
}
