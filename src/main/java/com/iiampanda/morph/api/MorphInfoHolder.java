package com.iiampanda.morph.api;

import com.iiampanda.morph.api.morph.MorphInfo;

/**
 * Duck interface implemented on Player by Morph's mixin. Replaces the Forge capability.
 */
public interface MorphInfoHolder
{
    MorphInfo morph$getMorphInfo();
}
