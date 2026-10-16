package ai.idealistic.spartan.abstraction.event;

import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import kireiko.dev.millennium.types.Vec2f;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class RotationEvent {
    private PlayerProtocol profile;
    private Vec2f from;
    private Vec2f to;

    // Shortest signed angle between two yaw values, as yaw wraps around at +-180 degrees
    private static float wrapDegrees(float degrees) {
        degrees %= 360.0f;

        if (degrees >= 180.0f) {
            degrees -= 360.0f;
        } else if (degrees < -180.0f) {
            degrees += 360.0f;
        }
        return degrees;
    }

    public Vec2f getDelta() {
        return new Vec2f(wrapDegrees(to.getX() - from.getX()), to.getY() - from.getY());
    }

    public Vec2f getAbsDelta() {
        Vec2f delta = this.getDelta();
        return new Vec2f(Math.abs(delta.getX()), Math.abs(delta.getY()));
    }
}
