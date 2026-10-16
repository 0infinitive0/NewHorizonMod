package newhorizon.util.ui.display;

import arc.graphics.Color;
import arc.math.Interp;
import arc.scene.actions.Actions;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.ui.Styles;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.content;

public class ItemDisplay extends Table{
    public final Item item;
    public final int amount;

    public ItemDisplay(Item item){
        this(item, 0);
    }

    public ItemDisplay(Item item, int amount, boolean showName){
        arc.scene.ui.layout.Stack stack = new arc.scene.ui.layout.Stack();
        stack.add(new arc.scene.ui.Image(item.uiIcon).setScaling(arc.util.Scaling.fit));
        if(amount > 0){
            Table amountTable = new Table().left().bottom();
            amountTable.add(amount >= 1000 ? (amount / 1000) + "k" : String.valueOf(amount)).style(Styles.outlineLabel);
            stack.add(amountTable);
        }
        add(stack).size(32f);
        if(showName) add(item.localizedName).padLeft(4 + amount > 99 ? 4 : 0);

        this.item = item;
        this.amount = amount;
    }

    public ItemDisplay(Item item, int amount){
        this(item, amount, true);
    }

    /** Displays the item with a "/sec" qualifier based on the time period, in ticks. */
    public ItemDisplay(Item item, int amount, float timePeriod, boolean showName){
        add(new ItemImage(item.uiIcon, amount));
        add((showName ? item.localizedName + "\n" : "") + "[lightgray]" + Strings.autoFixed(amount / (timePeriod / 60f), 2) + StatUnit.perSecond.localized()).padLeft(2).padRight(5).style(Styles.outlineLabel);

        this.item = item;
        this.amount = amount;
    }
}
