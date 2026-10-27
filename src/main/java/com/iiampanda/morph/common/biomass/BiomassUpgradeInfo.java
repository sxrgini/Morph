package com.iiampanda.morph.common.biomass;

import org.jetbrains.annotations.Nullable;

/** One node of the biomass upgrade tree (config/morph/biomass/*.json). */
public class BiomassUpgradeInfo
{
    /** Bumped when the bundled defaults change; older files in the config folder are replaced (the old one is kept as .old). */
    @Nullable public Integer schema;
    public String id;
    @Nullable public String parentId;
    @Nullable public Integer maxLevel;   // null = unlimited
    @Nullable public Integer freeLevels; // levels everyone starts with
    @Nullable public Multiplier value;   // null = one off unlock, no numeric value
    @Nullable public Multiplier cost;

    public int free()
    {
        return freeLevels == null ? 0 : freeLevels;
    }

    public static class Multiplier
    {
        public double baseValue;
        public Double incValue;
        public double multiplier = 1D;
        public int equation = 1;

        public double get(int level)
        {
            if(level <= 0)
            {
                return 0D;
            }
            double inc = incValue == null ? baseValue : incValue;
            return switch(equation)
            {
                case 2 -> baseValue + (inc * Math.pow(level - 1, multiplier));
                case 3 -> baseValue + (inc * Math.pow(multiplier, level - 1));
                case 4 -> baseValue + (inc * Math.log10(level * multiplier));
                case 5 -> baseValue + (inc * Math.sqrt((level - 1) * multiplier));
                default -> baseValue + (inc * multiplier * level);
            };
        }
    }
}
