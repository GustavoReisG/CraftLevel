package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Preenche pagamento ao selecionar troca; devolve esmeraldas ao fechar (ESC). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class MerchantWalletPaymentEvents {

    private static final Map<UUID, Integer> LAST_SELECTION = new ConcurrentHashMap<>();

    private MerchantWalletPaymentEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            LAST_SELECTION.remove(player.getUUID());
            return;
        }

        var container = MerchantMenuHelper.getTradeContainer(menu);
        if (container == null) {
            return;
        }

        int selection = MerchantMenuHelper.getSelectionHint(container);
        Integer previous = LAST_SELECTION.get(player.getUUID());
        if (previous != null && previous == selection) {
            return;
        }

        LAST_SELECTION.put(player.getUUID(), selection);
        EmeraldPaymentService.refillPaymentFromWallet(player, menu, MerchantMenuHelper.getSelectedOffer(menu));
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getContainer() instanceof MerchantMenu) {
            LAST_SELECTION.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        LAST_SELECTION.remove(player.getUUID());

        if (!(event.getContainer() instanceof MerchantMenu menu)) {
            return;
        }

        var container = MerchantMenuHelper.getTradeContainer(menu);
        EmeraldPaymentService.returnPaymentEmeraldsToWallet(player, container);
    }
}
