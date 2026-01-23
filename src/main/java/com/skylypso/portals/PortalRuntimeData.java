package com.skylypso.portals;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PortalRuntimeData implements Component<EntityStore>
{
    // You must set this from your plugin setup after registration.
    public static ComponentType<EntityStore, PortalRuntimeData> TYPE;

    public long lastTeleportTick = -999999;

    @Override
    public Component<EntityStore> clone() {
        PortalRuntimeData copy = new PortalRuntimeData();
        copy.lastTeleportTick = this.lastTeleportTick;
        return copy;
    }

    public static PortalRuntimeData get(PlayerRef playerRef)
    {
        Holder<EntityStore> holder = playerRef.getHolder();
        if (holder == null || TYPE == null) return null;
        return holder.ensureAndGetComponent(TYPE);
    }
}
