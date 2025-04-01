package com.cavetale.hive.mob;

import lombok.Getter;

@Getter
public abstract class AbstractSpawnMob implements SpawnMob {
    private int weight = 1;
    private int minWave = 1;
    private boolean shouldRespawn = true;

    /**
     * Set the weight.
     */
    public AbstractSpawnMob weight(int value) {
        weight = value;
        return this;
    }

    /**
     * Set the min wave.
     */
    public AbstractSpawnMob minWave(int value) {
        minWave = value;
        return this;
    }

    /**
     * Set the shouldRespawn flag.
     */
    public AbstractSpawnMob shouldRespawn(boolean value) {
        shouldRespawn = value;
        return this;
    }

    /**
     * Return the shouldRespawn flag.
     */
    @Override
    public boolean shouldRespawn() {
        return shouldRespawn;
    }
}
