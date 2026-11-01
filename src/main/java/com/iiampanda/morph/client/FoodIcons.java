package com.iiampanda.morph.client;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class FoodIcons
{
    private static final Map<String, String> FOODS = Map.ofEntries(
            Map.entry("minecraft:axolotl", "minecraft:tropical_fish"),
            Map.entry("minecraft:bee", "minecraft:poppy"),
            Map.entry("minecraft:camel", "minecraft:cactus"),
            Map.entry("minecraft:camel_husk", "minecraft:cactus"),
            Map.entry("minecraft:cat", "minecraft:salmon"),
            Map.entry("minecraft:ocelot", "minecraft:salmon"),
            Map.entry("minecraft:chicken", "minecraft:wheat_seeds"),
            Map.entry("minecraft:cow", "minecraft:wheat"),
            Map.entry("minecraft:mooshroom", "minecraft:red_mushroom"),
            Map.entry("minecraft:sheep", "minecraft:wheat"),
            Map.entry("minecraft:goat", "minecraft:tall_grass"),
            Map.entry("minecraft:horse", "minecraft:wheat"),
            Map.entry("minecraft:donkey", "minecraft:wheat"),
            Map.entry("minecraft:mule", "minecraft:wheat"),
            Map.entry("minecraft:skeleton_horse", "minecraft:bone"),
            Map.entry("minecraft:zombie_horse", "minecraft:dead_bush"),
            Map.entry("minecraft:llama", "minecraft:wheat"),
            Map.entry("minecraft:trader_llama", "minecraft:wheat"),
            Map.entry("minecraft:fox", "minecraft:salmon"),
            Map.entry("minecraft:frog", "minecraft:slime_ball"),
            Map.entry("minecraft:tadpole", "minecraft:kelp"),
            Map.entry("minecraft:hoglin", "minecraft:crimson_fungus"),
            Map.entry("minecraft:panda", "minecraft:bamboo"),
            Map.entry("minecraft:parrot", "minecraft:wheat_seeds"),
            Map.entry("minecraft:pig", "minecraft:carrot"),
            Map.entry("minecraft:rabbit", "minecraft:carrot"),
            Map.entry("minecraft:sniffer", "minecraft:torchflower"),
            Map.entry("minecraft:strider", "minecraft:warped_fungus"),
            Map.entry("minecraft:turtle", "minecraft:seagrass"),
            Map.entry("minecraft:wolf", "minecraft:bone"),
            Map.entry("minecraft:armadillo", "minecraft:spider_eye"),
            Map.entry("minecraft:happy_ghast", "minecraft:snowball"),
            Map.entry("minecraft:dolphin", "minecraft:cod"),
            Map.entry("minecraft:polar_bear", "minecraft:cod"),
            Map.entry("minecraft:villager", "minecraft:emerald"),
            Map.entry("minecraft:wandering_trader", "minecraft:emerald"),
            Map.entry("minecraft:iron_golem", "minecraft:iron_ingot"),
            Map.entry("minecraft:zombie_villager", "minecraft:rotten_flesh"),
            Map.entry("minecraft:zombie", "minecraft:rotten_flesh"),
            Map.entry("minecraft:husk", "minecraft:rotten_flesh"),
            Map.entry("minecraft:drowned", "minecraft:rotten_flesh"),
            Map.entry("minecraft:cod", "minecraft:ink_sac"),
            Map.entry("minecraft:salmon", "minecraft:frogspawn"),
            Map.entry("minecraft:tropical_fish", "minecraft:kelp"),
            Map.entry("minecraft:pufferfish", "minecraft:kelp"),
            Map.entry("minecraft:nautilus", "minecraft:pufferfish"),
            Map.entry("minecraft:zombie_nautilus", "minecraft:pufferfish"),
            Map.entry("minecraft:zombified_piglin", "minecraft:gold_nugget"),
            Map.entry("minecraft:piglin", "minecraft:gold_ingot"),
            Map.entry("minecraft:piglin_brute", "minecraft:gold_ingot"),
            Map.entry("minecraft:skeleton", "minecraft:bone"),
            Map.entry("minecraft:stray", "minecraft:bone"),
            Map.entry("minecraft:bogged", "minecraft:bone"),
            Map.entry("minecraft:wither_skeleton", "minecraft:coal"),
            Map.entry("minecraft:allay", "minecraft:amethyst_shard"),
            Map.entry("minecraft:spider", "minecraft:cobweb"),
            Map.entry("minecraft:cave_spider", "minecraft:cobweb"),
            Map.entry("minecraft:creeper", "minecraft:gunpowder"),
            Map.entry("minecraft:enderman", "minecraft:ender_pearl"),
            Map.entry("minecraft:slime", "minecraft:slime_ball"),
            Map.entry("minecraft:magma_cube", "minecraft:magma_cream"),
            Map.entry("minecraft:blaze", "minecraft:blaze_powder"),
            Map.entry("minecraft:ghast", "minecraft:fire_charge"),
            Map.entry("minecraft:phantom", "minecraft:phantom_membrane"),
            Map.entry("minecraft:bat", "minecraft:melon_slice"),
            Map.entry("minecraft:copper_golem", "minecraft:copper_ingot"),
            Map.entry("minecraft:glow_squid", "minecraft:cod"),
            Map.entry("minecraft:snow_golem", "minecraft:snowball"),
            Map.entry("minecraft:squid", "minecraft:cod"),
            Map.entry("minecraft:sulfur_cube", "minecraft:sulfur"),
            Map.entry("minecraft:breeze", "minecraft:breeze_rod"),
            Map.entry("minecraft:creaking", "minecraft:resin_clump"),
            Map.entry("minecraft:elder_guardian", "minecraft:cod"),
            Map.entry("minecraft:endermite", "minecraft:end_stone"),
            Map.entry("minecraft:evoker", "minecraft:emerald"),
            Map.entry("minecraft:guardian", "minecraft:cod"),
            Map.entry("minecraft:parched", "minecraft:bone"),
            Map.entry("minecraft:pillager", "minecraft:emerald"),
            Map.entry("minecraft:ravager", "minecraft:cooked_chicken"),
            Map.entry("minecraft:shulker", "minecraft:end_stone"),
            Map.entry("minecraft:silverfish", "minecraft:stone"),
            Map.entry("minecraft:vex", "minecraft:amethyst_shard"),
            Map.entry("minecraft:vindicator", "minecraft:emerald"),
            Map.entry("minecraft:warden", "minecraft:cooked_chicken"),
            Map.entry("minecraft:witch", "minecraft:glass_bottle"),
            Map.entry("minecraft:zoglin", "minecraft:crimson_fungus"),
            Map.entry("minecraft:ender_dragon", "minecraft:dragon_egg"),
            Map.entry("minecraft:wither", "minecraft:nether_star")
    );

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private FoodIcons() {}

    public static ItemStack stackFor(Player player)
    {
        MorphInfo info = MorphApi.getMorphInfo(player);
        if(!info.isMorphed())
        {
            return ItemStack.EMPTY;
        }
        LivingEntity e = info.getEntity();
        if(e == null)
        {
            return ItemStack.EMPTY;
        }
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        if(e instanceof net.minecraft.world.entity.animal.MushroomCow cow)
        {
            key = key + (String.valueOf(cow.getVariant()).toLowerCase(java.util.Locale.ROOT).contains("brown") ? "#brown" : "#red");
        }
        ItemStack cached = CACHE.get(key);
        if(cached == null)
        {
            cached = resolve(key, e);
            CACHE.put(key, cached);
        }
        return cached;
    }

    private static ItemStack resolve(String key, LivingEntity e)
    {
        String food = key.endsWith("#brown") ? "minecraft:brown_mushroom" : FOODS.get(key.replace("#red", ""));
        if(food != null)
        {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(food));
            if(item != null && item != net.minecraft.world.item.Items.AIR)
            {
                return new ItemStack(item);
            }
        }
        if(e instanceof Animal animal)
        {
            for(Item item : BuiltInRegistries.ITEM)
            {
                try
                {
                    ItemStack stack = new ItemStack(item);
                    if(!stack.isEmpty() && animal.isFood(stack))
                    {
                        return stack;
                    }
                }
                catch(RuntimeException ignored) {}
            }
        }
        return ItemStack.EMPTY;
    }

    public static void clear()
    {
        CACHE.clear();
    }
}
