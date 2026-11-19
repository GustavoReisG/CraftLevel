package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.item.ItemRunes;
import com.grgs.craftlevel.item.RuneKind;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Rune of Unbreakable: ao quebrar, trava em 1 de durabilidade e marca broken
 * (sem stats) em vez de destruir o item.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends LivingEntity> void craftlevel$unbreakable(
            int amount,
            T entity,
            Consumer<T> onBroken,
            CallbackInfo ci
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (amount <= 0 || !self.isDamageableItem() || !ItemRunes.has(self, RuneKind.UNBREAKABLE)) {
            return;
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        // Já quebrado: não gasta mais, não some.
        if (ItemRunes.isBroken(self)) {
            self.setDamageValue(Math.max(0, self.getMaxDamage() - 1));
            ci.cancel();
            return;
        }

        int damage = self.getDamageValue();
        int max = self.getMaxDamage();
        if (damage + amount < max) {
            return;
        }

        if (ItemRunes.tryPreventBreak(self)) {
            onBroken.accept(entity);
            ci.cancel();
        }
    }
}
