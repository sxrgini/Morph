package com.iiampanda.morph.api.mob.trait;

/**
 * Marks that a trait wants its event hook methods (see {@link Trait}: allowDamage, allowEffect, allowTarget, onInteractEntity)
 * called whilst it is active. On Forge this meant "register on the event bus"; here it registers the trait with {@link TraitHooks}.
 */
public interface IEventBusRequired
{}
