package com.iiampanda.morph.client;

import java.lang.reflect.Method;

/**
 * Player Animation Library (used by Better Combat) draws the player's own model in first person to show its swing animations.
 * When that happens the morph must not be drawn, so the animation's arms and weapon show instead of the mob. Reflection, because it is optional.
 */
public final class PalCompat
{
    private static boolean resolved;
    private static Method statePass;
    private static Method globalPass;

    private PalCompat() {}

    private static void resolve()
    {
        resolved = true;
        try
        {
            globalPass = Class.forName("com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode").getMethod("isFirstPersonPass");
        }
        catch(Throwable ignored) {}
        try
        {
            statePass = Class.forName("com.zigythebird.playeranim.accessors.IAvatarAnimationState").getMethod("playerAnimLib$isFirstPersonPass");
        }
        catch(Throwable ignored) {}
    }

    /** @param renderState the render state (26.x) or null (1.21.1, where the library keeps a global flag) */
    public static boolean isFirstPersonPass(Object renderState)
    {
        if(!resolved)
        {
            resolve();
        }
        try
        {
            if(statePass != null && renderState != null && statePass.getDeclaringClass().isInstance(renderState))
            {
                return (Boolean)statePass.invoke(renderState);
            }
            if(globalPass != null)
            {
                return (Boolean)globalPass.invoke(null);
            }
        }
        catch(Throwable ignored) {}
        return false;
    }

    private static Method bcManager;
    private static Method bcLayers;
    private static boolean bcResolved;

    /** Better Combat plays its attack as a Player Animation Library animation and never makes the player swing, so this looks for that animation. */
    public static boolean betterCombatAttacking(Object player)
    {
        try
        {
            if(!bcResolved)
            {
                bcResolved = true;
                for(String n : new String[] {"com.zigythebird.playeranim.accessors.IAnimatedAvatar", "com.zigythebird.playeranim.accessors.IAnimatedPlayer"})
                {
                    try
                    {
                        bcManager = Class.forName(n).getMethod("playerAnimLib$getAnimManager");
                        break;
                    }
                    catch(Throwable ignored) {}
                }
            }
            if(bcManager == null || !bcManager.getDeclaringClass().isInstance(player))
            {
                return false;
            }
            Object manager = bcManager.invoke(player);
            if(bcLayers == null)
            {
                bcLayers = manager.getClass().getMethod("getLayers");
            }
            for(Object pair : (java.util.List<?>)bcLayers.invoke(manager))
            {
                Object layer = null;
                for(String m : new String[] {"getRight", "getSecond", "right", "second", "getValue", "getB"})
                {
                    try
                    {
                        layer = pair.getClass().getMethod(m).invoke(pair);
                        break;
                    }
                    catch(Throwable ignored) {}
                }
                if(layer == null)
                {
                    continue;
                }
                for(Class<?> c = layer.getClass(); c != null; c = c.getSuperclass())
                {
                    if(c.getName().equals("net.bettercombat.client.animation.AttackAnimationStack"))
                    {
                        return (Boolean)layer.getClass().getMethod("isActive").invoke(layer);
                    }
                }
            }
        }
        catch(Throwable ignored) {}
        return false;
    }
}
