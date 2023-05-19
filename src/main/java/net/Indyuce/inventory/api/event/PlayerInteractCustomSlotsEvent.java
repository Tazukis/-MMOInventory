package net.Indyuce.inventory.api.event;

import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.inventory.InventoryItem;
import net.Indyuce.inventory.inventory.InventoryLookupMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Represents an event that is called when a player interacts with an object or
 * air.
 * <p>
 * This event will fire as cancelled if the vanilla behavior is to do nothing
 * (e.g interacting with air). For the purpose of avoiding doubt, this means
 * that the event will only be in the cancelled state if it is fired as a result
 * of some prediction made by the server where no subsequent code will run,
 * rather than when the subsequent interaction activity (e.g. placing a block in
 * an illegal position ({@link BlockCanBuildEvent}) will fail.
 */
public class PlayerInteractCustomSlotsEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    protected Collection<InventoryItem> items;
    protected Action action;
    protected Block blockClicked;
    protected BlockFace blockFace;
    private Result useClickedBlock;
    private Result useItemInHand;
    private InventoryHandler inventoryHandler;

    public PlayerInteractCustomSlotsEvent(@NotNull final InventoryHandler who, @NotNull final Action action, @Nullable final Block clickedBlock, @NotNull final BlockFace clickedFace) {
        super(who.getPlayer());
        this.inventoryHandler = who;
        this.action = action;
        this.items = who.getItems(InventoryLookupMode.NORMAL);
        this.blockClicked = clickedBlock;
        this.blockFace = clickedFace;

        useItemInHand = Result.DEFAULT;
        useClickedBlock = clickedBlock == null ? Result.DENY : Result.ALLOW;
    }

    /**
     * Returns the action type
     *
     * @return Action returns the type of interaction
     */
    @NotNull
    public Action getAction() {
        return action;
    }

    public InventoryHandler getInventoryHandler() {
        return inventoryHandler;
    }

    /**
     * Gets the cancellation state of this event. Set to true if you want to
     * prevent buckets from placing water and so forth
     *
     * @return boolean cancellation state
     * @deprecated This event has two possible cancellation states, one for
     *         {@link #useInteractedBlock()} and one for {@link #useItemInHand()}. It is
     *         possible a call might have the former false, but the latter true, eg in
     *         the case of using a firework whilst gliding. Callers should check the
     *         relevant methods individually.
     */
    @Deprecated
    @Override
    public boolean isCancelled() {
        return useInteractedBlock() == Result.DENY;
    }

    /**
     * Sets the cancellation state of this event. A canceled event will not be
     * executed in the server, but will still pass to other plugins
     * <p>
     * Canceling this event will prevent use of food (player won't lose the
     * food item), prevent bows/snowballs/eggs from firing, etc. (player won't
     * lose the ammo)
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel) {
        setUseInteractedBlock(cancel ? Result.DENY : useInteractedBlock() == Result.DENY ? Result.DEFAULT : useInteractedBlock());
        setUseItemInHand(cancel ? Result.DENY : useItemInHand() == Result.DENY ? Result.DEFAULT : useItemInHand());
    }

    /**
     * Returns the item in hand represented by this event
     *
     * @return ItemStack the item used
     */
    @Nullable
    public Collection<InventoryItem> getItems() {
        return this.items;
    }

    /**
     * Check if this event involved a block
     *
     * @return boolean true if it did
     */
    public boolean hasBlock() {
        return this.blockClicked != null;
    }

    /**
     * Returns the clicked block
     *
     * @return Block returns the block clicked with this item.
     */
    @Nullable
    public Block getClickedBlock() {
        return blockClicked;
    }

    /**
     * Returns the face of the block that was clicked
     *
     * @return BlockFace returns the face of the block that was clicked
     */
    @NotNull
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /**
     * This controls the action to take with the block (if any) that was
     * clicked on. This event gets processed for all blocks, but most don't
     * have a default action
     *
     * @return the action to take with the interacted block
     */
    @NotNull
    public Result useInteractedBlock() {
        return useClickedBlock;
    }

    /**
     * @param useInteractedBlock the action to take with the interacted block
     */
    public void setUseInteractedBlock(@NotNull Result useInteractedBlock) {
        this.useClickedBlock = useInteractedBlock;
    }

    /**
     * This controls the action to take with the item the player is holding.
     * This includes both blocks and items (such as flint and steel or
     * records). When this is set to default, it will be allowed if no action
     * is taken on the interacted block.
     *
     * @return the action to take with the item in hand
     */
    @NotNull
    public Result useItemInHand() {
        return useItemInHand;
    }

    /**
     * @param useItemInHand the action to take with the item in hand
     */
    public void setUseItemInHand(@NotNull Result useItemInHand) {
        this.useItemInHand = useItemInHand;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}