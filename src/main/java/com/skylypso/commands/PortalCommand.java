package com.skylypso.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.skylypso.SkylypsoPlugin;
import com.skylypso.portals.PortalLocation;
import com.skylypso.portals.PortalPair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class PortalCommand extends AbstractCommand
{
    private final RequiredArg<String> SUB;

    public PortalCommand()
    {
        super("portal", "Configure the PoC portal pair");
        // Important: you must register arguments on the command, otherwise
        // the parser will never populate context.get(...)
        this.SUB = this.withRequiredArg(
            "subcommand",
            "setentry|setexit|info|clear|swap",
            ArgTypes.STRING
        );
        this.setAllowsExtraArguments(true);
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context)
    {
        String sub = context.get(SUB);
        if (sub == null) sub = "info";
        sub = sub.toLowerCase();

        // Only players can set locations (needs position)
        Player senderPlayer = (context.sender() instanceof Player p) ? p : null;
        if ((sub.equals("setentry") || sub.equals("setexit")) && senderPlayer == null) {
            context.sender().sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        switch (sub) {
            case "setentry" -> {
                setHereAsync(context, true);
            }
            case "setexit" -> {
                setHereAsync(context, false);
            }
            case "swap" -> {
                PortalPair p = SkylypsoPlugin.get().portalPair.get();
                SkylypsoPlugin.get().portalPair.set(new PortalPair(p.exit(), p.entry()));
                SkylypsoPlugin.get().savePortalPair();
                context.sender().sendMessage(Message.raw("Swapped entry and exit."));
            }
            case "clear" -> {
                SkylypsoPlugin.get().portalPair.set(PortalPair.empty());
                SkylypsoPlugin.get().savePortalPair();
                context.sender().sendMessage(Message.raw("Cleared portal pair."));
            }
            case "info" -> {
                PortalPair p = SkylypsoPlugin.get().portalPair.get();
                context.sender().sendMessage(Message.raw(p.describe()));
            }
            default -> {
                context.sender().sendMessage(Message.raw("Unknown subcommand. Use: setentry, setexit, info, clear, swap"));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> setHereAsync(CommandContext context, boolean entry) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        Player player = context.senderAs(Player.class);
        World world = player.getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("You are not in a world right now."));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null) {
            context.sendMessage(Message.raw("Could not access your player entity reference."));
            return CompletableFuture.completedFuture(null);
        }

        // 1) Location im WorldThread lesen
        CompletableFuture<PortalLocation> locFuture = new CompletableFuture<>();

        world.execute(() -> {
            try {
                TransformComponent tc = tryGetTransformComponent(ref);
                if (tc == null) {
                    locFuture.completeExceptionally(
                        new IllegalStateException("Missing TransformComponent")
                    );
                    return;
                }

                Vector3d pos = tc.getPosition();
                int bx = (int) Math.floor(pos.x);
                int by = (int) Math.floor(pos.y - 0.1f);
                int bz = (int) Math.floor(pos.z);

                PortalLocation loc = new PortalLocation(world.getName(), bx, by, bz);
                locFuture.complete(loc);
            } catch (Throwable t) {
                locFuture.completeExceptionally(t);
            }
        });

        // 2) State + File save NICHT im WorldThread machen
        return locFuture.handle((loc, err) -> {
            if (err != null) {
                context.sendMessage(Message.raw("Could not read your position: " + err.getMessage()));
                return null;
            }

            PortalPair old = SkylypsoPlugin.get().portalPair.get();
            PortalPair updated = entry
                ? new PortalPair(loc, old.exit())
                : new PortalPair(old.entry(), loc);

            SkylypsoPlugin.get().portalPair.set(updated);

            // Save lieber off-thread (Command läuft ohnehin off-thread)
            SkylypsoPlugin.get().savePortalPair();

            context.sendMessage(Message.raw(
                (entry ? "Entry" : "Exit") + " set to " + loc.world() + " @ " + loc.x() + "," + loc.y() + "," + loc.z()
            ));
            return null;
        });
    }

    private TransformComponent tryGetTransformComponent(Ref<EntityStore> ref) {
        Store<EntityStore> store = ref.getStore(); // <- Store<EntityStore>, nicht EntityStore
        return store.getComponent(ref, TransformComponent.getComponentType());
    }
}
