package mod.torchbowmod;


import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static mod.torchbowmod.TorchBow.TORCH_ITEMS;

@Mod(TorchBowMod.MODID)
public class TorchBowMod {
    public static final String MODID = "torchbowmod";
    public static Logger LOGGER = LogManager.getLogger("TorchBowMod");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    //@ObjectHolder(registryName = "ceilingtorch:torch", value = "ceilingtorch")
    public static Block CeilingTorch = null;

    public static DeferredItem<Item> torchbow = ITEMS.register("torchbow", () -> new TorchBow(new Item.Properties().durability(384)));
    public static DeferredItem<Item> multiTorch = ITEMS.register("multitorch", () -> new Item(new Item.Properties().stacksTo(64)));
    public static DeferredItem<Item> torchArrow = ITEMS.register("torcharrow", () -> new TorchArrow(new Item.Properties().stacksTo(64)));
    public static DeferredHolder<EntityType<?>, EntityType<EntityTorch>> entityTorch = ENTITY_TYPES.register("entitytorch", () ->
            EntityType.Builder.<EntityTorch>of(EntityTorch::new, MobCategory.MISC)
                    .setTrackingRange(60)
                    .setUpdateInterval(5)
                    .setShouldReceiveVelocityUpdates(true)
                    .sized(0.5F, 0.5F)
                    .build("entitytorch"));
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> torchTab = TAB.register("torchbowmodtab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.torchBowModTab"))
                    .icon(() -> new ItemStack(torchbow.get()))
                    .displayItems((parameters,output) -> {
                        output.accept(torchbow.get());
                        output.accept(multiTorch.get());
                        output.accept(torchArrow.get());
                    }).build());

    public static final Map<BlockItem, WallTorchBlock> ITEM_TO_WALL_BLOCK = new HashMap<>();

    public TorchBowMod(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        TAB.register(modEventBus);
        modEventBus.addListener(this::initClient);
        modEventBus.addListener(this::preInit);
    }

    private void initClient(final FMLClientSetupEvent event) {
        event.enqueueWork(() ->
        {
            ItemProperties.register(torchbow.get(),
                    ResourceLocation.withDefaultNamespace("pull"), (itemStack, world, livingEntity, num) -> {
                        if (livingEntity == null) {
                            return 0.0F;
                        } else {
                            return livingEntity.getUseItem() != itemStack ? 0.0F : (float) (itemStack.getUseDuration(livingEntity) - livingEntity.getUseItemRemainingTicks()) / 20.0F;
                        }
                    });
            ItemProperties.register(torchbow.get(), ResourceLocation.withDefaultNamespace("pulling"), (itemStack, world, livingEntity, num)
                    -> livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F);
        });
    }

    private void preInit(final FMLCommonSetupEvent event) {
        CeilingTorch = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ceilingtorch", "torch"));
        event.enqueueWork(() -> {
            Map<String, Integer> modCountMap = new HashMap<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block instanceof WallTorchBlock wallBlock) {
                    Item asItem = block.asItem();
                    if (asItem instanceof BlockItem blockItem) {
                        ITEM_TO_WALL_BLOCK.put(blockItem, wallBlock);
                        TORCH_ITEMS.add(blockItem);
                        String namespace = BuiltInRegistries.ITEM.getKey(asItem).getNamespace();
                        modCountMap.merge(namespace, 1, Integer::sum);
                    }
                }
            }
            LOGGER.info("==== TorchBowMod Torch Item Auto-Registration Stats ====");
            LOGGER.info("Total registered pairs: {}", ITEM_TO_WALL_BLOCK.size());
            for (Map.Entry<String, Integer> entry : modCountMap.entrySet()) {
                LOGGER.info("Namespace '{}' has {} torch items", entry.getKey(), entry.getValue());
            }
            LOGGER.info("========================================================");
        });
    }

    @Mod(value = TorchBowMod.MODID, dist = Dist.CLIENT)
    @EventBusSubscriber(modid = TorchBowMod.MODID, value = Dist.CLIENT)
    public static class TorchBowModClient {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(TorchBowMod.entityTorch.get(), RenderTorch::new);
        }
    }
}

