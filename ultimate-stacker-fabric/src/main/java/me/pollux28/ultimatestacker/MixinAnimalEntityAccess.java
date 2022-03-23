package me.pollux28.ultimatestacker;

import net.minecraft.entity.passive.AnimalEntity;

import java.util.List;

public interface MixinAnimalEntityAccess<T extends AnimalEntity> {
    List<T> getInLove();
    void setInLove(List<T> inLove);
    void addToInLove(T animalEntity);
    void addToInLove(List<T> animalEntityList);
}
