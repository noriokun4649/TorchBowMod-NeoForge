package mod.torchbowmod;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TorchRenderState extends ArrowRenderState {
    public BlockState blockState;
}
