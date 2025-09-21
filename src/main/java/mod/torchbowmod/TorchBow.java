package mod.torchbowmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static mod.torchbowmod.TorchBowMod.*;

public class TorchBow extends ProjectileWeaponItem {
    public static final Set<BlockItem> TORCH_ITEMS = new HashSet<>();

    public static final Predicate<ItemStack> TORCH = itemStack -> itemStack.getItem() instanceof BlockItem bi && ITEM_TO_WALL_BLOCK.containsKey(bi);
    public static final Predicate<ItemStack> MULTI_TORCH = itemStack -> itemStack.is(multiTorch.get());
    public static final Predicate<ItemStack> TORCH_ARROW = itemStack -> itemStack.is(torchArrow.get());
    public static final Predicate<ItemStack> TORCH_BOW_ONLY;

    private class Offsets {
        private final float X;
        private final float Y;

        Offsets(float x,float y){
            this.X = x;
            this.Y = y;
        }
    }

    static {
        TORCH_BOW_ONLY = TORCH.or(MULTI_TORCH).or(TORCH_ARROW);
    }

    public TorchBow(Item.Properties p_40660_) {
        super(p_40660_);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (entityLiving instanceof Player player) {
            ItemStack itemstack = player.getProjectile(stack);
            if (!itemstack.isEmpty()) {
                int i = this.getUseDuration(stack, entityLiving) - timeLeft;
                i = net.neoforged.neoforge.event.EventHooks.onArrowLoose(stack, level, player, i, !itemstack.isEmpty());
                if (i < 0) return;
                float f = getPowerForTime(i);
                if (!((double)f < 0.1)) {
                    List<ItemStack> list = draw(stack, itemstack, player);
                    if (level instanceof ServerLevel serverlevel && !list.isEmpty()) {
                        if (list.getFirst().is(multiTorch.get())){
                            ItemStack item = list.getFirst().copy();
                            list.addAll(Collections.nCopies(8, item));
                        }
                        this.shoot(serverlevel, player, player.getUsedItemHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
                    }

                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.ARROW_SHOOT,
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F
                    );
                    player.awardStat(Stats.ITEM_USED.get(this));
                }
            }
        }
    }

    @Override
    protected void shootProjectile(@NotNull LivingEntity livingEntity, @NotNull Projectile projectile, int i, float v, float v1, float v2, @Nullable LivingEntity livingEntity1) {
        float offsetX = 0F;
        float offsetY = 0F;
        if (i < 9){
            float range = 10F;
            Offsets[] offsets = {
                    new Offsets(0F,0F),
                    new Offsets(-range, -range),
                    new Offsets(-range, 0.0F),
                    new Offsets(-range, range),
                    new Offsets(0.0F, -range),
                    new Offsets(0.0F, range),
                    new Offsets(range, -range),
                    new Offsets(range, 0.0F),
                    new Offsets(range, range)
            };
            offsetX = offsets[i].X;
            offsetY = offsets[i].Y;
        }
        projectile.shootFromRotation(livingEntity, livingEntity.getXRot() + offsetX, livingEntity.getYRot() + offsetY + v2, 0.0F, v, v1);
    }

    public static float getPowerForTime(int i) {
        float f = (float)i / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack, @NotNull LivingEntity livingEntity) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public @NotNull ItemStack getDefaultCreativeAmmo(@Nullable Player player, @NotNull ItemStack projectileWeaponItem) {
        return Blocks.TORCH.asItem().getDefaultInstance();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        boolean flag = !player.getProjectile(itemstack).isEmpty();

        InteractionResultHolder<ItemStack> ret = net.neoforged.neoforge.event.EventHooks.onArrowNock(itemstack, level, player, usedHand, flag);
        if (ret != null) return ret;

        if (!player.hasInfiniteMaterials() && !flag) {
            return InteractionResultHolder.fail(itemstack);
        } else {
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(itemstack);
        }
    }

    public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
        return TORCH_BOW_ONLY;
    }

    public int getDefaultProjectileRange() {
        return 15;
    }

    @Override
    protected @NotNull Projectile createProjectile(@NotNull Level worldIn, @NotNull LivingEntity livingEntity, @NotNull ItemStack weaponStack, ItemStack pickupItem, boolean p_336242_) {
        if (pickupItem.is(multiTorch.get())) pickupItem = Items.TORCH.getDefaultInstance();
        EntityTorch abstractedly = new EntityTorch(worldIn, livingEntity, pickupItem.copyWithCount(1), weaponStack);
        return abstractedly;
    }
}
