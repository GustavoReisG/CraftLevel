package net.minecraft.world.item.trading;

import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
   public MerchantOffers() {
   }

   private MerchantOffers(int p_220323_) {
      super(p_220323_);
   }

   public MerchantOffers(CompoundTag p_45387_) {
      ListTag listtag = p_45387_.m_128437_("Recipes", 10);

      for(int i = 0; i < listtag.size(); ++i) {
         this.add(new MerchantOffer(listtag.m_128728_(i)));
      }

   }

   @Nullable
   public MerchantOffer m_45389_(ItemStack p_45390_, ItemStack p_45391_, int p_45392_) {
      if (p_45392_ > 0 && p_45392_ < this.size()) {
         MerchantOffer merchantoffer1 = this.get(p_45392_);
         return merchantoffer1.m_45355_(p_45390_, p_45391_) ? merchantoffer1 : null;
      } else {
         for(int i = 0; i < this.size(); ++i) {
            MerchantOffer merchantoffer = this.get(i);
            if (merchantoffer.m_45355_(p_45390_, p_45391_)) {
               return merchantoffer;
            }
         }

         return null;
      }
   }

   public void m_45393_(FriendlyByteBuf p_45394_) {
      p_45394_.m_236828_(this, (p_220325_, p_220326_) -> {
         p_220325_.m_130055_(p_220326_.m_45352_());
         p_220325_.m_130055_(p_220326_.m_45368_());
         p_220325_.m_130055_(p_220326_.m_45364_());
         p_220325_.writeBoolean(p_220326_.m_45380_());
         p_220325_.writeInt(p_220326_.m_45371_());
         p_220325_.writeInt(p_220326_.m_45373_());
         p_220325_.writeInt(p_220326_.m_45379_());
         p_220325_.writeInt(p_220326_.m_45377_());
         p_220325_.writeFloat(p_220326_.m_45378_());
         p_220325_.writeInt(p_220326_.m_45375_());
      });
   }

   public static MerchantOffers m_45395_(FriendlyByteBuf p_45396_) {
      return p_45396_.m_236838_(MerchantOffers::new, (p_220328_) -> {
         ItemStack itemstack = p_220328_.m_130267_();
         ItemStack itemstack1 = p_220328_.m_130267_();
         ItemStack itemstack2 = p_220328_.m_130267_();
         boolean flag = p_220328_.readBoolean();
         int i = p_220328_.readInt();
         int j = p_220328_.readInt();
         int k = p_220328_.readInt();
         int l = p_220328_.readInt();
         float f = p_220328_.readFloat();
         int i1 = p_220328_.readInt();
         MerchantOffer merchantoffer = new MerchantOffer(itemstack, itemstack2, itemstack1, i, j, k, f, i1);
         if (flag) {
            merchantoffer.m_45381_();
         }

         merchantoffer.m_45359_(l);
         return merchantoffer;
      });
   }

   public CompoundTag m_45388_() {
      CompoundTag compoundtag = new CompoundTag();
      ListTag listtag = new ListTag();

      for(int i = 0; i < this.size(); ++i) {
         MerchantOffer merchantoffer = this.get(i);
         listtag.add(merchantoffer.m_45384_());
      }

      compoundtag.m_128365_("Recipes", listtag);
      return compoundtag;
   }
}