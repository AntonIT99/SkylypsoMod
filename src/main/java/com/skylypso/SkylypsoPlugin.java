package com.skylypso;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.skylypso.commands.PortalCommand;
import com.skylypso.portals.PortalLocation;
import com.skylypso.portals.PortalPair;
import com.skylypso.portals.PortalRuntimeData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SkylypsoPlugin extends JavaPlugin
{
    public final AtomicReference<PortalPair> portalPair = new AtomicReference<>(PortalPair.empty());

    private static SkylypsoPlugin instance;
    private Path dataFile;

    private static final long COOLDOWN_TICKS = 20; // ~0.66s at 30 TPS (world default)

    public SkylypsoPlugin(@Nonnull JavaPluginInit init)
    {
        super(init);
    }

    public static SkylypsoPlugin get()
    {
        return instance;
    }

    @Override
    protected void setup()
    {
        instance = this;

        /*getCommandRegistry().registerCommand(new ExampleCommand("example", "An example command"));
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);*/

        // Where we store the pair (super simple JSON)
        dataFile = Path.of("mods", "SkylypsoPlugin", "portal.json");
        loadPortalPair();

        // Commands
        getCommandRegistry().registerCommand(new PortalCommand());

        getLogger().atInfo().log("SkylypsoPlugin setup complete.");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void start()
    {
        // Repeating check task (runs off-thread -> we hop to world thread with world.execute())
        ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            () -> {
                try {
                    tickPortals();
                } catch (Throwable t) {
                    // Wenn hier nichts loggst, wirkt es so als "läuft der Task nicht"
                    getLogger().atSevere().withCause(t).log("tickPortals crashed");
                }
            },
            0, 100, TimeUnit.MILLISECONDS // 10x per second is plenty for PoC
        );

        getTaskRegistry().registerTask((ScheduledFuture<Void>) task);

        getLogger().atInfo().log("SkylypsoPlugin started.");
    }

    @Override
    protected void shutdown()
    {
        savePortalPair();
        getLogger().atInfo().log("SkylypsoPlugin shutting down.");
    }

    private void tickPortals() {
        PortalPair pair = portalPair.get();
        if (!pair.isComplete()) return;

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            UUID worlduuid = playerRef.getWorldUuid();
            if (worlduuid == null)
                continue;
            World world = Universe.get().getWorld(worlduuid);
            if (world == null) continue;

            world.execute(() -> {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null) return;

                Store<EntityStore> store = ref.getStore(); // <- IM WorldThread ok

                // Optional: Player brauchst du hier eigentlich nicht, außer für world (haben wir schon).
                // Wenn du ihn brauchst:
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) return;

                Transform t = playerRef.getTransform();
                if (t == null) return;

                int bx = (int) Math.floor(getX(t));
                int by = (int) Math.floor(getY(t) - 0.1);
                int bz = (int) Math.floor(getZ(t));

                long nowTick = world.getTick();

                PortalRuntimeData data = PortalRuntimeData.get(playerRef);
                if (data != null && (nowTick - data.lastTeleportTick) < COOLDOWN_TICKS) return;

                boolean onEntry = pair.entry().matches(world.getName(), bx, by, bz);
                boolean onExit  = pair.exit().matches(world.getName(), bx, by, bz);
                if (!onEntry && !onExit) return;

                PortalLocation dest = onEntry ? pair.exit() : pair.entry();
                Vector3d targetPos = new Vector3d(dest.x() + 1.5, dest.y() + 1.01, dest.z() + 0.5);
                Vector3f targetRot = new Vector3f(0, 0, 0);

                Teleport teleport = Teleport.createForPlayer(world, targetPos, targetRot);
                store.addComponent(ref, Teleport.getComponentType(), teleport);

                if (data != null) data.lastTeleportTick = nowTick;
            });
        }
    }

    // --- Transform helpers (adjust to your actual Transform API) ---
    public static double getX(Transform t)
    {
        return t.getPosition().getX();
    }

    public static double getY(Transform t)
    {
        return t.getPosition().getY();
    }

    public static double getZ(Transform t)
    {
        return t.getPosition().getZ();
    }

    // --- Persistence (simple JSON; replace with official config APIs when you prefer) ---
    private void loadPortalPair() {
        try {
            if (!Files.exists(dataFile)) return;
            String json = Files.readString(dataFile);
            portalPair.set(PortalPair.fromJson(json));
            getLogger().atInfo().log("Loaded portal pair from disk.");
        } catch (IOException e) {
            getLogger().atWarning().withCause(e).log("Failed to load portal.json");
        }
    }

    public void savePortalPair() {
        try {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, portalPair.get().toJson());
            getLogger().atInfo().log("Saved portal pair to disk.");
        } catch (IOException e) {
            getLogger().atWarning().withCause(e).log("Failed to save portal.json");
        }
    }
}