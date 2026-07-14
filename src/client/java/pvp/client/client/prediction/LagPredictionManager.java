package pvp.client.client.prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pvp.client.client.modules.LagGhostModule;

public final class LagPredictionManager {
	private static final Map<Integer, Track> TRACKS = new HashMap<>();
	private static final double MIN_HORIZONTAL_MOMENTUM = 0.055;
	private static final double MIN_VERTICAL_MOMENTUM = 0.025;
	private static final long MIN_FREEZE_NANOS = 110_000_000L;
	private static int correctionCooldown;

	private LagPredictionManager() {
	}

	public static void movementPacket(Entity entity) {
		if (!LagGhostModule.INSTANCE.isEnabled()) {
			return;
		}
		if (!(entity instanceof AbstractClientPlayer player) || player == Minecraft.getInstance().player) {
			return;
		}
		long now = System.nanoTime();
		Vec3 packetPos = entity.getPositionCodec().getBase();
		Track track = TRACKS.computeIfAbsent(entity.getId(), ignored -> new Track(entity.getId(), packetPos, now, player.onGround()));
		track.authoritativeUpdate(player, packetPos, now, false);
	}

	public static void motionPacket(Entity entity) {
		if (!LagGhostModule.INSTANCE.isEnabled()) {
			return;
		}
		if (entity instanceof AbstractClientPlayer player && player != Minecraft.getInstance().player) {
			Track track = TRACKS.get(entity.getId());
			if (track != null) {
				track.velocity = entity.getDeltaMovement();
				track.observedHorizontal = track.velocity.horizontal();
			}
		}
	}

	public static void correctionPacket(Entity entity) {
		if (!LagGhostModule.INSTANCE.isEnabled()) {
			return;
		}
		if (entity instanceof AbstractClientPlayer player) {
			Track track = TRACKS.get(entity.getId());
			if (track != null) {
				track.hardReset(player, entity.getPositionCodec().getBase(), System.nanoTime(), 20);
			}
		}
	}

	public static void localCorrection() {
		if (!LagGhostModule.INSTANCE.isEnabled()) {
			return;
		}
		correctionCooldown = 20;
		for (Track track : TRACKS.values()) {
			track.disableFor(20);
		}
	}

	public static void tick(Minecraft minecraft, LagGhostModule config) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			clear();
			return;
		}
		if (correctionCooldown > 0) {
			correctionCooldown--;
		}

		long now = System.nanoTime();
		Set<Integer> present = new HashSet<>();
		for (AbstractClientPlayer player : level.players()) {
			if (player == minecraft.player) {
				continue;
			}
			present.add(player.getId());
			Track track = TRACKS.computeIfAbsent(player.getId(), ignored -> new Track(player.getId(), player.position(), now, player.onGround()));
			track.tick(player, level, now, config);
		}
		TRACKS.keySet().removeIf(id -> !present.contains(id));
	}

	public static List<RenderView> renderViews(float partialTick) {
		if (!LagGhostModule.INSTANCE.isEnabled()) {
			return List.of();
		}
		List<RenderView> result = new ArrayList<>();
		for (Track track : TRACKS.values()) {
			if (track.rendering && track.opacity > 0.01F) {
				result.add(new RenderView(track.entityId, track.previousGhostPos.lerp(track.ghostPos, partialTick), track.opacity));
			}
		}
		return result;
	}

	public static void clear() {
		TRACKS.clear();
		correctionCooldown = 0;
	}

	private static boolean unsupported(AbstractClientPlayer player) {
		return player.isPassenger()
			|| player.isFallFlying()
			|| player.isSwimming()
			|| player.isInWater()
			|| player.isInLava()
			|| player.onClimbable()
			|| player.isSpectator()
			|| player.getAbilities().flying;
	}

	public record RenderView(int entityId, Vec3 position, float opacity) {
	}

	private static final class Track {
		private final int entityId;
		private Vec3 lastPacketPos;
		private Vec3 simPos;
		private Vec3 velocity = Vec3.ZERO;
		private Vec3 observedHorizontal = Vec3.ZERO;
		private Vec3 previousMeasuredHorizontal = Vec3.ZERO;
		private Vec3 ghostPos;
		private Vec3 previousGhostPos;
		private long lastPacketNanos;
		private long averageIntervalNanos = 150_000_000L;
		private long intervalJitterNanos = 20_000_000L;
		private int disableTicks;
		private int predictionTicks;
		private int movingSamples;
		private int airborneSamples;
		private double momentumQuality = 1.0;
		private boolean simulatedOnGround;
		private boolean airborneMomentum;
		private boolean pathCollided;
		private boolean rendering;
		private boolean reconciling;
		private float opacity;

		private Track(int entityId, Vec3 pos, long now, boolean onGround) {
			this.entityId = entityId;
			this.lastPacketPos = pos;
			this.simPos = pos;
			this.ghostPos = pos;
			this.previousGhostPos = pos;
			this.lastPacketNanos = now;
			this.simulatedOnGround = onGround;
		}

		private void authoritativeUpdate(AbstractClientPlayer player, Vec3 pos, long now, boolean correction) {
			long interval = Math.max(1L, now - this.lastPacketNanos);
			long cadenceLimit = Math.max(250_000_000L, (long)(this.averageIntervalNanos * 2.5));
			if (!this.rendering && interval <= cadenceLimit) {
				long deviation = Math.abs(interval - this.averageIntervalNanos);
				this.intervalJitterNanos = (long)(this.intervalJitterNanos * 0.75 + deviation * 0.25);
				this.averageIntervalNanos = (long)(this.averageIntervalNanos * 0.75 + interval * 0.25);
			}
			double ticks = Math.clamp(interval / 50_000_000.0, 1.0, 10.0);
			Vec3 delta = pos.subtract(this.lastPacketPos);
			boolean changedPosition = delta.lengthSqr() > 1.0E-7;
			if (!changedPosition && !correction) {
				if (!this.rendering) {
					this.velocity = this.velocity.multiply(0.35, 1.0, 0.35);
					this.observedHorizontal = this.observedHorizontal.scale(0.35);
					this.movingSamples = 0;
					this.momentumQuality = 0.0;
					this.lastPacketNanos = now;
				}
				this.airborneMomentum = !player.onGround() || Math.abs(this.velocity.y()) >= MIN_VERTICAL_MOMENTUM;
				return;
			}
			if (delta.lengthSqr() > 64.0 || correction) {
				this.disableFor(20);
				this.velocity = Vec3.ZERO;
			} else {
				Vec3 measured = delta.scale(1.0 / ticks);
				Vec3 measuredHorizontal = measured.horizontal();
				double measuredSpeed = measuredHorizontal.horizontalDistance();
				double previousSpeed = this.previousMeasuredHorizontal.horizontalDistance();
				if (measuredSpeed >= MIN_HORIZONTAL_MOMENTUM) {
					double quality = 1.0;
					if (previousSpeed >= MIN_HORIZONTAL_MOMENTUM) {
						quality = Math.min(measuredSpeed, previousSpeed) / Math.max(measuredSpeed, previousSpeed);
					}
					this.momentumQuality = this.movingSamples == 0
						? quality
						: this.momentumQuality * 0.6 + quality * 0.4;
					this.movingSamples = Math.min(10, this.movingSamples + 1);
					this.previousMeasuredHorizontal = measuredHorizontal;
				} else {
					this.movingSamples = 0;
					this.momentumQuality = 0.0;
				}
				Vec3 oldHorizontal = this.velocity.horizontal();
				double oldSpeed = oldHorizontal.horizontalDistance();
				double direction = measuredSpeed >= MIN_HORIZONTAL_MOMENTUM && oldSpeed >= MIN_HORIZONTAL_MOMENTUM
					? measuredHorizontal.dot(oldHorizontal) / (measuredSpeed * oldSpeed)
					: 1.0;
				double horizontalBlend = direction < 0.5 ? 1.0 : 0.65;
				double blendedX = this.velocity.x() * (1.0 - horizontalBlend) + measured.x() * horizontalBlend;
				double blendedZ = this.velocity.z() * (1.0 - horizontalBlend) + measured.z() * horizontalBlend;
				double blendedY = this.velocity.y() * 0.35 + measured.y() * 0.65;
				this.velocity = new Vec3(blendedX, blendedY, blendedZ);
				this.observedHorizontal = this.velocity.horizontal();
				this.airborneMomentum = !player.onGround() || Math.abs(measured.y()) >= MIN_VERTICAL_MOMENTUM;
				this.airborneSamples = this.airborneMomentum ? Math.min(10, this.airborneSamples + 1) : 0;
			}

			if (this.rendering) {
				this.reconciling = true;
			} else {
				this.ghostPos = pos;
				this.previousGhostPos = pos;
			}
			this.lastPacketPos = pos;
			this.simPos = pos;
			this.lastPacketNanos = now;
			this.simulatedOnGround = player.onGround();
			this.pathCollided = false;
			this.predictionTicks = 0;
		}

		private void tick(AbstractClientPlayer player, ClientLevel level, long now, LagGhostModule config) {
			this.previousGhostPos = this.ghostPos;
			if (this.disableTicks > 0) {
				this.disableTicks--;
			}
			if (correctionCooldown > 0 || unsupported(player)) {
				this.disableFor(4);
				this.anchorTo(player, player.getPositionCodec().getBase(), now);
				return;
			}

			if (this.reconciling) {
				double durationTicks = Math.max(1.0, config.reconciliationTime.get() * 20.0);
				double remaining = Math.exp(-3.0 / durationTicks);
				this.ghostPos = this.ghostPos.lerp(player.position(), 1.0 - remaining);
				this.opacity *= (float)remaining;
				if (this.ghostPos.distanceToSqr(player.position()) < 0.0025 || this.opacity < 0.02F) {
					this.rendering = false;
					this.reconciling = false;
					this.opacity = 0.0F;
				}
			}

			this.simulate(player, level, config);
			long sincePacket = now - this.lastPacketNanos;
			long learnedThreshold = (long)(this.averageIntervalNanos * 1.65 + this.intervalJitterNanos * 2.5);
			long threshold = Math.max(MIN_FREEZE_NANOS, (long)(learnedThreshold * config.detectionDelay.get()));
			boolean timingEvidence = sincePacket > threshold
				&& (NetworkTiming.silenceNanos(now) > MIN_FREEZE_NANOS || sincePacket > (long)(threshold * 1.6));
			double divergence = this.simPos.distanceTo(player.position());
			double horizontalMomentum = this.observedHorizontal.horizontalDistance();
			int requiredSamples = config.motionSamples.getI();
			boolean reliableHorizontal = horizontalMomentum >= MIN_HORIZONTAL_MOMENTUM
				&& this.movingSamples >= requiredSamples
				&& this.momentumQuality >= config.directionStability.get();
			boolean reliableAirborne = (this.airborneMomentum || Math.abs(this.velocity.y()) >= MIN_VERTICAL_MOMENTUM)
				&& this.airborneSamples >= Math.min(2, requiredSamples);
			boolean hasPredictableMomentum = reliableHorizontal || reliableAirborne;
			boolean dynamicCollision = !level.getEntityCollisions(player, predictedBox(player).inflate(0.04)).isEmpty();

			if (!this.rendering && this.disableTicks == 0 && timingEvidence && !this.pathCollided && !dynamicCollision
				&& hasPredictableMomentum && divergence >= config.activationDistance.get()) {
				this.rendering = true;
				this.reconciling = false;
				this.ghostPos = this.simPos;
				this.previousGhostPos = this.simPos;
				this.predictionTicks = 0;
			}

			if (this.rendering && !this.reconciling) {
				this.predictionTicks++;
				this.ghostPos = this.simPos;
				double elapsedMs = this.predictionTicks * 50.0;
				float confidence = (float)Math.max(config.minimumOpacity.get(), 1.0 - elapsedMs / config.duration.get());
				if (dynamicCollision) {
					confidence *= 0.55F;
				}
				this.opacity = config.opacity.getF() * confidence;
			}
		}

		private void simulate(AbstractClientPlayer player, ClientLevel level, LagGhostModule config) {
			AABB box = predictedBox(player);
			Vec3 requested = this.velocity;
			Vec3 resolved = Entity.collideBoundingBox(player, requested, box, level, List.of());
			boolean hitX = Math.abs(requested.x() - resolved.x()) > 1.0E-5;
			boolean hitY = Math.abs(requested.y() - resolved.y()) > 1.0E-5;
			boolean hitZ = Math.abs(requested.z() - resolved.z()) > 1.0E-5;
			this.simPos = this.simPos.add(resolved);
			AABB movedBox = box.move(resolved);
			Vec3 supportProbe = Entity.collideBoundingBox(player, new Vec3(0.0, -0.08, 0.0), movedBox, level, List.of());
			boolean landed = hitY && requested.y() < 0.0;
			this.simulatedOnGround = landed || supportProbe.y() > -0.07999;
			if ((hitX || hitZ) && !this.rendering) {
				this.pathCollided = true;
			}

			double vx = hitX ? 0.0 : resolved.x();
			double vz = hitZ ? 0.0 : resolved.z();
			double vy = hitY ? 0.0 : resolved.y();
			float blockFriction = this.simulatedOnGround
				? level.getBlockState(BlockPos.containing(this.simPos.x(), this.simPos.y() - 0.5000001, this.simPos.z())).getBlock().getFriction()
				: 1.0F;
			double horizontalFriction = blockFriction * 0.91F;
			vx = vx * horizontalFriction + this.observedHorizontal.x() * (1.0 - horizontalFriction);
			vz = vz * horizontalFriction + this.observedHorizontal.z() * (1.0 - horizontalFriction);
			if (!this.simulatedOnGround) {
				vy = (vy - 0.08) * 0.98;
			}
			this.velocity = new Vec3(vx, vy, vz);
		}

		private AABB predictedBox(AbstractClientPlayer player) {
			return player.getBoundingBox().move(this.simPos.subtract(player.position()));
		}

		private void hardReset(AbstractClientPlayer player, Vec3 pos, long now, int ticks) {
			this.disableFor(ticks);
			this.anchorTo(player, pos, now);
		}

		private void anchorTo(AbstractClientPlayer player, Vec3 pos, long now) {
			this.lastPacketPos = pos;
			this.simPos = pos;
			this.ghostPos = pos;
			this.previousGhostPos = pos;
			this.lastPacketNanos = now;
			this.velocity = Vec3.ZERO;
			this.observedHorizontal = Vec3.ZERO;
			this.previousMeasuredHorizontal = Vec3.ZERO;
			this.simulatedOnGround = player.onGround();
			this.airborneMomentum = false;
			this.pathCollided = false;
			this.predictionTicks = 0;
		}

		private void disableFor(int ticks) {
			this.disableTicks = Math.max(this.disableTicks, ticks);
			this.rendering = false;
			this.reconciling = false;
			this.opacity = 0.0F;
			this.pathCollided = false;
			this.movingSamples = 0;
			this.airborneSamples = 0;
			this.momentumQuality = 0.0;
		}
	}
}
