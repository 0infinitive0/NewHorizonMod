package newhorizon.expand.units.unitType;

import mindustry.graphics.MultiPacker;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import newhorizon.content.NHItems;
import newhorizon.util.func.GridUtil;

public class NHUnitType extends UnitType {
    public NHUnitType(String name) {
        super(name);

    }

    @Override
    public void init() {
        super.init();
        float maxWeaponRange = 0;
        for (Weapon weapon : weapons) {
            if (weapon.range() > maxWeaponRange) {
                maxWeaponRange = weapon.range();
            }
        }
        fogRadius = maxWeaponRange / 8;
    }

    @Override
    public void createIcons(MultiPacker packer) {
        super.createIcons(packer);
        GridUtil.register(this);
    }

    public void setRequirements(ItemStack[] stacks) {
        cachedRequirements = stacks;
        totalRequirements = firstRequirements = ItemStack.mult(stacks, 1 / 15f);
    }
}
