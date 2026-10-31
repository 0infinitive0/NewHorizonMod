package newhorizon.expand.block.cargo;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import newhorizon.expand.hiemara.CargoCap;

/**
 * Lists every cargo unit of the team, bound or not, wherever it is, and lets the player free it from its port or recycle
 * it. Binding is done at the port itself.
 */
public class CargoManager extends Block{
	public CargoManager(String name){
		super(name);
		
		solid = true;
		//a block needs one of these to have a building at all
		destructible = true;
		configurable = true;
		placeableLiquid = true;
		
		//A unit to unbind.
		config(Integer.class, (ManagerBuild build, Integer id) -> HubBlock.unbindUnit(build.team, id));
		//A unit to recycle.
		config(Long.class, (ManagerBuild build, Long id) -> HubBlock.recycleUnit(build.team, id.intValue()));
	}
	
	public class ManagerBuild extends Building{
		@Override
		public void buildConfiguration(Table table){
			table.button(Icon.units, Styles.cleari, this::openDialog).size(50f).tooltip("@manager.open");
		}
		
		protected void openDialog(){
			BaseDialog dialog = new BaseDialog("@manager.title");
			Table content = new Table();
			
			Runnable[] rebuild = {null};
			rebuild[0] = () -> {
				content.clear();
				content.top().left();
				
				content.add(Core.bundle.format("bar.cargocap", CargoCap.count(team), CargoCap.capString(team))).color(Pal.accent).left().row();
				content.image().color(Pal.accent).height(3f).growX().pad(4f).row();
				
				Seq<Unit> units = new Seq<>();
				for(Unit unit : team.data().units){
					if(CargoCap.isCargo(unit.type))units.add(unit);
				}
				units.sort(unit -> unit.type.id * 100000f + unit.id);
				
				if(units.isEmpty())content.add("@manager.none").color(Color.lightGray).left().row();
				for(Unit unit : units){
					HubBlock.HubBuild hub = HubBlock.boundHub(unit);
					content.table(Styles.grayPanel, row -> {
						row.image(unit.type.uiIcon).size(40f).scaling(Scaling.fit).pad(6f);
						row.add(unit.type.localizedName + " #" + unit.id).left().growX();
						row.add(HubBlock.loadText(unit)).color(Color.lightGray).padRight(8f);
						row.add(hub == null ? Core.bundle.get("manager.free") : Core.bundle.format("manager.bound", hub.tileX(), hub.tileY())).color(hub == null ? Color.lightGray : Pal.accent).padRight(8f);
						row.button("@port.unbind", Styles.defaultt, () -> {
							configure(Integer.valueOf(unit.id));
							rebuild[0].run();
						}).size(110f, 40f).disabled(b -> hub == null).pad(4f);
						row.button("@port.recycle", Styles.defaultt, () -> HubBlock.confirmRecycle(unit, () -> {
							configure(Long.valueOf(unit.id));
							rebuild[0].run();
						})).size(110f, 40f).pad(4f);
					}).growX().padBottom(4f).row();
				}
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
			
			dialog.cont.pane(content).grow().minWidth(620f).minHeight(300f);
			dialog.addCloseButton();
			dialog.show();
			deselect();
		}
	}
}
