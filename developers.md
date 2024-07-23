# Developer's Guide

## Introduction

This guide provides detailed documentation and examples for the custom events in the `com.ranull.graves.event` package. These events are used to handle various interactions with graves in the game.

## Events

### 1. GraveBlockPlaceEvent

The `GraveBlockPlaceEvent` class represents an event where a block associated with a grave is placed in the world. This event is cancellable, meaning it can be prevented from occurring by event listeners.

#### Methods

- `Grave getGrave()`: Gets the grave associated with the event.
- `Location getLocation()`: Gets the location where the block is being placed.
- `void setLocation(Location location)`: Sets the location where the block is being placed.
- `Block getBlock()`: Gets the block at the location where the block is being placed.
- `BlockData.BlockType getBlockType()`: Gets the type of the block being placed.
- `boolean isCancelled()`: Checks whether the event is cancelled.
- `void setCancelled(boolean cancel)`: Sets whether the event should be cancelled.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveBlockPlaceEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveBlockPlace(GraveBlockPlaceEvent event) {
        Grave grave = event.getGrave();
        Location location = event.getLocation();
        BlockData.BlockType blockType = event.getBlockType();

        if (shouldCancelPlacement(grave, location, blockType)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelPlacement(Grave grave, Location location, BlockData.BlockType blockType) {
        // Custom logic to determine if the event should be cancelled
        return false;
    }
}
```

### 2. GraveBreakEvent

The `GraveBreakEvent` class represents an event where a grave block is broken by a player. This event extends the `BlockBreakEvent` and includes additional information about the grave and whether items should drop upon breaking the grave block.

#### Methods

- `Grave getGrave()`: Gets the grave associated with the event.
- `int getBlockExp()`: Gets the experience points associated with breaking the grave.
- `boolean isDropItems()`: Checks whether items should drop upon breaking the grave block.
- `void setDropItems(boolean dropItems)`: Sets whether items should drop upon breaking the grave block.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveBreakEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveBreak(GraveBreakEvent event) {
        Grave grave = event.getGrave();
        Player player = event.getPlayer();
        Block block = event.getBlock();
        boolean dropItems = event.isDropItems();

        if (shouldPreventItemDrop(grave, player)) {
            event.setDropItems(false);
        }
    }

    private boolean shouldPreventItemDrop(Grave grave, Player player) {
        // Custom logic to determine if item drops should be prevented
        return false;
    }
}
```

### 3. GraveCloseEvent

The `GraveCloseEvent` class represents an event that occurs when an inventory associated with a grave is closed. This event extends the `InventoryCloseEvent` and includes additional information about the grave.

#### Methods

- `Grave getGrave()`: Gets the grave associated with the event.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveCloseEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveClose(GraveCloseEvent event) {
        Grave grave = event.getGrave();
        InventoryView inventoryView = event.getView();

        handleGraveClose(grave, inventoryView);
    }

    private void handleGraveClose(Grave grave, InventoryView inventoryView) {
        // Implement custom logic for when a grave inventory is closed
    }
}
```

### 4. GraveCreateEvent

The `GraveCreateEvent` class represents an event where a grave is created for an entity. This event is cancellable, meaning it can be prevented from occurring by event listeners.

#### Methods

- `Entity getEntity()`: Gets the entity for which the grave is being created.
- `EntityType getEntityType()`: Gets the type of the entity for which the grave is being created.
- `Grave getGrave()`: Gets the grave being created.
- `boolean isCancelled()`: Checks whether the event is cancelled.
- `void setCancelled(boolean cancel)`: Sets whether the event should be cancelled.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveCreateEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveCreate(GraveCreateEvent event) {
        Entity entity = event.getEntity();
        Grave grave = event.getGrave();

        if (shouldCancelGraveCreation(entity, grave)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelGraveCreation(Entity entity, Grave grave) {
        // Custom logic to determine if the event should be cancelled
        return false;
    }
}
```

### 5. GraveExplodeEvent

The `GraveExplodeEvent` class represents an event where a grave explodes. This event is cancellable, meaning it can be prevented from occurring by event listeners.

#### Methods

- `Location getLocation()`: Gets the location of the explosion.
- `Grave getGrave()`: Gets the grave that is exploding.
- `Entity getEntity()`: Gets the entity that caused the explosion, if any.
- `boolean isCancelled()`: Checks whether the event is cancelled.
- `void setCancelled(boolean cancel)`: Sets whether the event should be cancelled.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveExplodeEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveExplode(GraveExplodeEvent event) {
        Location location = event.getLocation();
        Grave grave = event.getGrave();
        Entity entity = event.getEntity();

        if (shouldCancelExplosion(grave, entity, location)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelExplosion(Grave grave, Entity entity, Location location) {
        // Custom logic to determine if the event should be cancelled
        return false;
    }
}
```

### 6. GraveOpenEvent

The `GraveOpenEvent` class represents an event where an inventory associated with a grave is opened. This event extends the `InventoryOpenEvent` and includes additional information about the grave.

#### Methods

- `Grave getGrave()`: Gets the grave associated with the event.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveOpenEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveOpen(GraveOpenEvent event) {
        Grave grave = event.getGrave();
        InventoryView inventoryView = event.getView();

        handleGraveOpen(grave, inventoryView);
    }

    private void handleGraveOpen(Grave grave, InventoryView inventoryView) {
        // Implement custom logic for when a grave inventory is opened
    }
}
```

### 7. GraveTimeoutEvent

The `GraveTimeoutEvent` class represents an event that occurs when a grave times out. This event is cancellable, meaning it can be prevented from occurring by event listeners.

#### Methods

- `Grave getGrave()`: Gets the grave that is timing out.
- `Location getLocation()`: Gets the location of the grave that is timing out.
- `void setLocation(Location location)`: Sets the location of the grave that is timing out.
- `boolean isCancelled()`: Checks whether the event is cancelled.
- `void setCancelled(boolean cancel)`: Sets whether the event should be cancelled.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveTimeoutEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveTimeout(GraveTimeoutEvent event) {
        Grave grave = event.getGrave();
        Location location = event.getLocation();

        if (shouldCancelTimeout(grave, location)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelTimeout(Grave grave, Location location) {
        // Custom logic to determine if the event should be cancelled
        return false;
    }
}
```

### 8. GraveAutoLootEvent

The `GraveAutoLootEvent` class represents an event that occurs when a grave is automatically looted. This event is cancellable, meaning it can be prevented from occurring by event listeners.

#### Methods

- `Grave getGrave()`: Gets the grave that is being looted.
- `Location getLocation()`: Gets the location of the grave that is being looted.
- `void setLocation(Location location)`: Sets the location of the grave that is being looted.
- `boolean isCancelled()`: Checks whether the event is cancelled.
- `void setCancelled(boolean cancel)`: Sets whether the event should be cancelled.
- `@Nullable Entity getEntity()`: Gets the entity that caused the event, if any.

#### Example Usage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ranull.graves.event.GraveAutoLootEvent;

public class GraveEventListener implements Listener {

    @EventHandler
    public void onGraveAutoLoot(GraveAutoLootEvent event) {
        Grave grave = event.getGrave();
        Location location = event.getLocation();
        Entity entity = event.getEntity();

        if (shouldCancelLoot(grave, location, entity)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelLoot(Grave grave, Location location, Entity entity) {
        // Custom logic to determine if the event should be cancelled
        return false;
    }
}
```