package com.iiampanda.morph.api;

import java.util.UUID;

/**
 * Duck interface implemented on every Entity by Morph's mixin. Marks entity instances that are used purely as the "body" of a morphing player.
 * (Replaces Forge's entity persistent data tag Morph used to use.)
 */
public interface MorphEntityAccess
{
    UUID morph$getMorphPlayerId();

    void morph$setMorphPlayerId(UUID id);

    default boolean morph$isMorphInstance()
    {
        return morph$getMorphPlayerId() != null;
    }
}
