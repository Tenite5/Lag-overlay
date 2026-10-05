# Predicted Lag Overlay

Predicted Lag Overlay is a client-side Fabric mod for Minecraft 26.3.

When movement updates stop, the mod continues a short visual simulation from the player's last known motion. If the prediction separates from the frozen player, a transparent copy shows where normal Minecraft movement would probably have carried them. It fades as confidence drops and rejoins the real player when updates return.

The predicted copy is render only. It is not an entity, has no hitbox, and cannot affect attacks, raycasts, targeting, collisions, interactions, or network packets.

## Detection

Prediction requires delayed movement packets and recent usable momentum. Block collisions, support states, nearby entity collisions, teleports, and unusual corrections are checked before a copy appears. Vehicles, elytra flight, swimming, climbing, spectator mode, and flight disable prediction.

These are the default settings:

| Setting | Default |
| --- | ---: |
| Activation Gap | 0.25 blocks |
| Packet Delay | 0.50x |
| Motion Samples | 2 |
| Momentum Quality | 0.50 |
| Ghost Opacity | 0.70 |
| Min Confidence | 0.35 |
| Confidence Fade | 900 ms |
| Rejoin Time | 0.10 s |

Press `K` to open the prediction settings. The key can be changed in Minecraft's Controls menu under Predicted Lag Overlay. The settings screen includes a Reset to Defaults button.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.3 or newer
- Fabric API 0.161.0+26.3
- Java 25

## Building

Run:

```text
./gradlew build
```

The JAR is written to `build/libs`. A local `local.properties` file can set `deployModsDir` to move successful builds directly to a test profile. This file is ignored by Git.

## License

Predicted Lag Overlay is available under the MIT License. See [LICENSE](LICENSE).
