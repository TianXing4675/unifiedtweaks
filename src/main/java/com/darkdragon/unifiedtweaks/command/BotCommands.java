package com.darkdragon.unifiedtweaks.command;

import com.darkdragon.unifiedtweaks.bind.HardBindManager;
import com.darkdragon.unifiedtweaks.bot.BotManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class BotCommands {
    private BotCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("utbot")
                            .requires(src -> src.hasPermission(2)) // OP 级别，后面你可以做更细的权限控制
                            .then(Commands.literal("spawn")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .executes(ctx -> spawnAtInvoker(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                    )
                            )
                            .then(Commands.literal("kill")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .executes(ctx -> {
                                                String name = StringArgumentType.getString(ctx, "name");
                                                if (!BotManager.isBotOnline(name)) {
                                                    ctx.getSource().sendFailure(Component.literal("Bot not found: " + name));
                                                    return 0;
                                                }
                                                BotManager.killBot(ctx.getSource().getServer(), name, "Killed by command");
                                                ctx.getSource().sendSuccess(() -> Component.literal("Killed bot: " + name), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(Commands.literal("list")
                                    .executes(ctx -> {
                                        var bots = BotManager.getAllBots().keySet();
                                        ctx.getSource().sendSuccess(() -> Component.literal("Bots: " + String.join(", ", bots)), false);
                                        return bots.size();
                                    })
                            )
                            .then(Commands.literal("hardbind")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .executes(ctx -> {
                                                var controller = ctx.getSource().getPlayerOrException();
                                                var bot = BotManager.getBotByName(StringArgumentType.getString(ctx, "name"));
                                                if (bot == null) {
                                                    ctx.getSource().sendFailure(Component.literal("Bot not found."));
                                                    return 0;
                                                }
                                                HardBindManager.hardBind(controller, bot);
                                                ctx.getSource().sendSuccess(() -> Component.literal("Hard-bound to bot."), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(Commands.literal("hardunbind")
                                    .executes(ctx -> {
                                        var controller = ctx.getSource().getPlayerOrException();
                                        HardBindManager.hardUnbind(controller);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Hard-unbound."), false);
                                        return 1;
                                    })
                            )

            );
        });
    }

    private static int spawnAtInvoker(CommandSourceStack src, String name) {
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("此命令不可以由控制台使用."));
            return 0;
        }

//        ServerLevel level = player.level();
//        BlockPos pos = player.blockPosition();

        try {
            BotManager.spawnBot(src.getPlayer(), name);
        } catch (Exception ex) {
            src.sendFailure(Component.literal("无法生成bot: " + ex.getMessage()));
            return 0;
        }

//        src.sendSuccess(() -> Component.literal("成功生成bot: " + name), false);
        return 1;
    }
}
