package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Remove o cap vanilla de 40 níveis na bigorna quando {@code anvil.uncapped_cost} está ativo. */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private static int craftlevel$uncapCostLimit(int vanilla) {
        return ConfigParser.anvilUncappedCost ? Integer.MAX_VALUE : vanilla;
    }
}
