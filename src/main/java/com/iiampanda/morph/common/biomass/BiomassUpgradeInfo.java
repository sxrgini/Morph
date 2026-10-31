package com.iiampanda.morph.common.biomass;

import org.jetbrains.annotations.Nullable;

public class BiomassUpgradeInfo
{

    @Nullable public Integer schema;
    public String id;
    @Nullable public String parentId;
    @Nullable public Integer maxLevel;
    @Nullable public Integer freeLevels;
    @Nullable public Multiplier value;
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
