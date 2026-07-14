package pvp.client.client.mixin;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import pvp.client.client.prediction.GhostRenderContext;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
	private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

	@Redirect(
		method = RENDER_LAYERS,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
	)
	private RenderType predictedLagOverlay$transparentGhostArmor(Identifier texture) {
		return GhostRenderContext.active() ? RenderTypes.armorTranslucent(texture) : RenderTypes.armorCutoutNoCull(texture);
	}

	@ModifyArg(
		method = RENDER_LAYERS,
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
		),
		index = 6
	)
	private int predictedLagOverlay$ghostArmorOpacity(int color) {
		return GhostRenderContext.active()
			? ARGB.color((int)(GhostRenderContext.opacity() * 255.0F), color)
			: color;
	}
}
