package pvp.client.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pvp.client.client.prediction.GhostRenderContext;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Shadow public abstract Identifier getTextureLocation(LivingEntityRenderState state);

	@Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
	private void predictedLagOverlay$ghostRenderType(
		LivingEntityRenderState state,
		boolean bodyVisible,
		boolean forceTransparent,
		boolean glowing,
		CallbackInfoReturnable<RenderType> cir
	) {
		if (GhostRenderContext.active()) {
			cir.setReturnValue(RenderTypes.entityTranslucentCullItemTarget(this.getTextureLocation(state)));
		}
	}

	@Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
	private void predictedLagOverlay$ghostOpacity(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
		if (GhostRenderContext.active()) {
			cir.setReturnValue(ARGB.color((int)(GhostRenderContext.opacity() * 255.0F), 255, 255, 255));
		}
	}

	@Redirect(
		method = "submit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
		)
	)
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void predictedLagOverlay$renderGhostArmorOnly(
		RenderLayer layer,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int lightCoords,
		EntityRenderState state,
		float yRot,
		float xRot
	) {
		if (!GhostRenderContext.active() || layer instanceof HumanoidArmorLayer) {
			layer.submit(poseStack, collector, lightCoords, state, yRot, xRot);
		}
	}
}
