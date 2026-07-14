package pvp.client.client.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pvp.client.client.prediction.NetworkTiming;

@Mixin(Connection.class)
public class ConnectionMixin {
	@Inject(method = "channelRead0", at = @At("HEAD"))
	private void predictedLagOverlay$recordInboundPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getConnection() != null && minecraft.getConnection().getConnection() == (Object)this) {
			NetworkTiming.inboundPacket();
		}
	}
}
