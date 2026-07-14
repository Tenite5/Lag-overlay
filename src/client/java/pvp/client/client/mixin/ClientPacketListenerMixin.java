package pvp.client.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pvp.client.client.prediction.LagPredictionManager;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Shadow private ClientLevel level;

	@Inject(method = "handleMoveEntity", at = @At("TAIL"))
	private void predictedLagOverlay$movementUpdate(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
		Entity entity = packet.getEntity(this.level);
		if (entity != null && packet.hasPosition()) {
			LagPredictionManager.movementPacket(entity);
		}
	}

	@Inject(method = "handleEntityPositionSync", at = @At("TAIL"))
	private void predictedLagOverlay$positionSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
		Entity entity = this.level.getEntity(packet.id());
		if (entity != null) {
			LagPredictionManager.movementPacket(entity);
		}
	}

	@Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
	private void predictedLagOverlay$motionUpdate(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
		Entity entity = this.level.getEntity(packet.id());
		if (entity != null) {
			LagPredictionManager.motionPacket(entity);
		}
	}

	@Inject(method = "handleTeleportEntity", at = @At("TAIL"))
	private void predictedLagOverlay$teleport(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
		Entity entity = this.level.getEntity(packet.id());
		if (entity != null) {
			LagPredictionManager.correctionPacket(entity);
		}
	}

	@Inject(method = "handleMovePlayer", at = @At("TAIL"))
	private void predictedLagOverlay$localCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
		LagPredictionManager.localCorrection();
	}
}
