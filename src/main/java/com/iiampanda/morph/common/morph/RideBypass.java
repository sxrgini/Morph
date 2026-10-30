package com.iiampanda.morph.common.morph;

public final class RideBypass
{
    public static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private RideBypass() {}
}
