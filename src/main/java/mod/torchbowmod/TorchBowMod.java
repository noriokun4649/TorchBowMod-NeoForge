package mod.torchbowmod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static mod.torchbowmod.TorchBow.TORCH_ITEMS;

@Mod(TorchBowMod.MODID)
public class TorchBowMod {
    public static final String MODID = "torchbowmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static Block CeilingTorch = null;
    public static Block CeilingSoulTorch = null;

    public static final ResourceLocation TORCH_BOW_ID = ResourceLocation.fromNamespaceAndPath(MODID, "torchbow");
    public static final ResourceLocation MULCH_TORCH_ID = ResourceLocation.fromNamespaceAndPath(MODID, "multitorch");
    public static final ResourceLocation TORCH_ARROW_ID = ResourceLocation.fromNamespaceAndPath(MODID, "torcharrow");
    public static final ResourceLocation TORCH_ENTITY = ResourceLocation.fromNamespaceAndPath(MODID,"entitytorch");
    public static final ResourceKey<Item> TORCH_BOW_KEY = ResourceKey.create(Registries.ITEM, TORCH_BOW_ID);
    public static final ResourceKey<Item> MULCH_TORCH_KEY = ResourceKey.create(Registries.ITEM, MULCH_TORCH_ID);
    public static final ResourceKey<Item> TORCH_ARROW_KEY = ResourceKey.create(Registries.ITEM, TORCH_ARROW_ID);
    public static final ResourceKey<EntityType<?>> TORCH_ENTITY_ID = ResourceKey.create(Registries.ENTITY_TYPE, TORCH_ENTITY);
    public static DeferredItem<Item> torchbow = ITEMS.register("torchbow", () -> new TorchBow(new Item.Properties().setId(TORCH_BOW_KEY).durability(384)));
    public static DeferredItem<Item> multiTorch = ITEMS.register("multitorch", () -> new Item(new Item.Properties().setId(MULCH_TORCH_KEY).stacksTo(64)));
    public static DeferredItem<Item> torchArrow = ITEMS.register("torcharrow", () -> new TorchArrow(new Item.Properties().setId(TORCH_ARROW_KEY).stacksTo(64)));
    public static DeferredHolder<EntityType<?>, EntityType<EntityTorch>> entityTorch = ENTITY_TYPES.register("entitytorch", () ->
            EntityType.Builder.<EntityTorch>of(EntityTorch::new, MobCategory.MISC)
                    .setTrackingRange(60)
                    .setUpdateInterval(5)
                    .setShouldReceiveVelocityUpdates(true)
                    .sized(0.5F, 0.5F)
                    .build(TORCH_ENTITY_ID));

    public static final Map<BlockItem, WallTorchBlock> ITEM_TO_WALL_BLOCK = new HashMap<>();

    public TorchBowMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        TAB.register(modEventBus);
        modEventBus.addListener(this::initClient);
        modEventBus.addListener(this::preInit);
        TAB.register("torchbowmodtab", () ->
                CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.torchBowModTab"))
                        .icon(() -> new ItemStack(torchbow.get()))
                        .displayItems((parameters,output) -> {
                            output.accept(torchbow.get());
                            output.accept(multiTorch.get());
                            output.accept(torchArrow.get());
                        }).build());
    }

    private void initClient(final FMLClientSetupEvent event) {
    }

    private void preInit(final FMLCommonSetupEvent event) {
        CeilingTorch = BuiltInRegistries.BLOCK.getValue(ResourceLocation.fromNamespaceAndPath("ceilingtorch", "torch"));
        CeilingSoulTorch = BuiltInRegistries.BLOCK.getValue(ResourceLocation.fromNamespaceAndPath("ceilingtorch", "soul_torch"));
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
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(TorchBowMod.entityTorch.get(), RenderTorch::new);
        }
    }
}

