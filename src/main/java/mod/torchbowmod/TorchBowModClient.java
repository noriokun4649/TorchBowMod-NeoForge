package mod.torchbowmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = TorchBowMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TorchBowMod.MODID, value = Dist.CLIENT)
public class TorchBowModClient {
    public TorchBowModClient(IEventBus modEventBus, ModContainer modContainer) {

    }
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TorchBowMod.entityTorch.get(), RenderTorch::new);
    }
}
