package pvp.client.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pvp.client.client.prediction.GhostRenderContext;
import pvp.client.client.prediction.LagPredictionManager;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

	@Inject(method = "submitEntities", at = @At("TAIL"))
	private void predictedLagOverlay$submitLagGhosts(
		PoseStack poseStack,
		LevelRenderState levelState,
		SubmitNodeCollector output,
		CallbackInfo ci
	) {
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		List<LagPredictionManager.RenderView> ghosts = LagPredictionManager.renderViews(partialTick);
		if (ghosts.isEmpty()) {
			return;
		}
		Vec3 camera = levelState.cameraRenderState.pos;
		for (LagPredictionManager.RenderView ghost : ghosts) {
			AvatarRenderState state = findPlayerState(levelState, ghost.entityId());
			if (state == null) {
				continue;
			}
			Component nameTag = state.nameTag;
			boolean fire = state.displayFireAnimation;
			int outlineColor = state.outlineColor;
			List<EntityRenderState.ShadowPiece> shadows = new ArrayList<>(state.shadowPieces);
			state.nameTag = null;
			state.displayFireAnimation = false;
			state.outlineColor = 0;
			state.shadowPieces.clear();
			Vec3 pos = ghost.position();
			try {
				GhostRenderContext.run(ghost.opacity(), () -> this.entityRenderDispatcher.submit(
					state,
					levelState.cameraRenderState,
					pos.x() - camera.x(),
					pos.y() - camera.y(),
					pos.z() - camera.z(),
					poseStack,
					output
				));
			} finally {
				state.nameTag = nameTag;
				state.displayFireAnimation = fire;
				state.outlineColor = outlineColor;
				state.shadowPieces.addAll(shadows);
			}
		}
	}

	private static AvatarRenderState findPlayerState(LevelRenderState levelState, int entityId) {
		for (EntityRenderState state : levelState.entityRenderStates) {
			if (state instanceof AvatarRenderState avatar && avatar.id == entityId) {
				return avatar;
			}
		}
		return null;
	}
}
