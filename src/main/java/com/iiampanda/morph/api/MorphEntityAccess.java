package com.iiampanda.morph.api;

import java.util.UUID;

public interface MorphEntityAccess
{
    UUID morph$getMorphPlayerId();

    void morph$setMorphPlayerId(UUID id);

    default boolean morph$isMorphInstance()
    {
        return morph$getMorphPlayerId() != null;
    }
}
