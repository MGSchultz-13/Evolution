package tgw.evolution.util.collection.lists;

import tgw.evolution.Evolution;

public interface ListEv {

    boolean CHECKS = true;

    void clear();

    default void deprecatedMethod() {
        if (CHECKS) {
            Evolution.deprecatedMethod();
        }
    }

    default void reset() {
        this.clear();
        this.trim();
    }

    void trim();
}
