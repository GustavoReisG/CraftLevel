package com.grgs.craftlevel.altar;

import com.grgs.craftlevel.block.AltarOfDeathBlockEntity;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.config.AltarTradeParser;
import com.grgs.craftlevel.glory.GloryKillsService;
import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Catálogo e compra da aba Trade. */
public final class AltarTradeService {

    private AltarTradeService() {}

    public static List<AltarTradeOffer> offers() {
        return AltarTradeParser.offers();
    }

    public static int offerCount() {
        return offers().size();
    }

    public static AltarTradeOffer offerAt(int index) {
        List<AltarTradeOffer> list = offers();
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    /**
     * Compra 1 unidade da oferta.
     * {@code toInventory}: shift — vai ao inventário; senão acumula no cursor do menu.
     */
    public static boolean tryBuy(
            ServerPlayer player,
            AltarOfDeathMenu menu,
            BlockPos altarPos,
            int offerIndex,
            boolean toInventory
    ) {
        AltarTradeOffer offer = offerAt(offerIndex);
        if (offer == null) {
            return false;
        }

        ItemStack purchased = offer.resultCopy();
        if (purchased.isEmpty()) {
            return false;
        }

        ItemStack carried = menu.getCarried();
        if (!toInventory) {
            if (!carried.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(carried, purchased)
                        || carried.getCount() >= carried.getMaxStackSize()) {
                    return false;
                }
            }
        }

        int cost = offer.gloryCost();
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY).map(glory -> {
            int gloryKills = glory.getGloryKills();
            if (gloryKills < cost) {
                player.displayClientMessage(
                        Component.translatable(
                                "block.craftlevel.altar_of_death.need_kills",
                                cost,
                                gloryKills
                        ),
                        true
                );
                return false;
            }

            glory.setGloryKills(gloryKills - cost);

            if (toInventory) {
                player.getInventory().placeItemBackInInventory(purchased);
            } else if (carried.isEmpty()) {
                menu.setCarried(purchased);
            } else {
                carried.grow(purchased.getCount());
                menu.setCarried(carried);
            }

            menu.broadcastChanges();
            GloryKillsService.sync(player);
            playBuyEffects(player.level(), altarPos);
            return true;
        }).orElse(false);
    }

    private static void playBuyEffects(Level level, BlockPos pos) {
        AltarOfDeathBlockEntity.applyExchangeDarkness(level, pos);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7F, 1.15F);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.2F);
    }
}
