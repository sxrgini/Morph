package com.iiampanda.morph.common;

public final class Dbg
{
    private Dbg() {}

    public static void log(String message, Object... args)
    {
        Morph.LOGGER.info("[MorphDebug] " + message, args);
    }
}
