package com.vagdedes.spartan.utils.minecraft.math.client.impl;

import com.vagdedes.spartan.utils.minecraft.math.client.ClientMath;
import com.vagdedes.spartan.utils.minecraft.mcp.MathHelper;

public class VanillaMath implements ClientMath {
    @Override
    public float sin(float value) {
        return MathHelper.sin(value);
    }

    @Override
    public float cos(float value) {
        return MathHelper.cos(value);
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }
}