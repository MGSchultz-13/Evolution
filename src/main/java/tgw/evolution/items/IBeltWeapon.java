package tgw.evolution.items;

/**
 * Implemented Items will be rendered in the player's belt
 */
public interface IBeltWeapon {

    /**
     * @return The priority of the item when rendering, 0 being the most priority, bigger numbers being lower priority.
     */
    int getPriority();
}
