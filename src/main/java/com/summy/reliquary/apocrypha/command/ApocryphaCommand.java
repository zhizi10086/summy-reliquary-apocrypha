package com.summy.reliquary.apocrypha.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 本模组的指令（目前只有一条）：{@code /summyapocrypha fear players <on|off>}。
 *
 * <p>它只做一件事：运行时切换「恐惧是否对玩家生效」这个配置键（{@code fear_affects_players}）。
 * 之所以用指令而不是只放配置：玩家档属于可选行为（会把玩家定身并封掉近战与右击），
 * 需要一个不用重启就能开关的入口；指令改的就是配置值，因此重启后依然生效。
 */
public final class ApocryphaCommand {
	private ApocryphaCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> players = Commands.literal("players")
				.requires(source -> source.hasPermission(2))
				.executes(ApocryphaCommand::query);
		players.then(Commands.literal("on")
				.executes(context -> set(context, true)));
		players.then(Commands.literal("off")
				.executes(context -> set(context, false)));

		event.getDispatcher().register(Commands.literal("summyapocrypha")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("fear").then(players)));
	}

	private static int query(CommandContext<CommandSourceStack> context) {
		boolean current = ApocryphaConfig.apoptosisFearAffectsPlayers();
		context.getSource().sendSuccess(() -> Component.translatable(
				"command.summy-reliquary-apocrypha.fear.players.query", onOff(current)), false);
		return 1;
	}

	private static int set(CommandContext<CommandSourceStack> context, boolean value) {
		ApocryphaConfig.setApoptosisFearAffectsPlayers(value);
		context.getSource().sendSuccess(() -> Component.translatable(
				value ? "command.summy-reliquary-apocrypha.fear.players.on"
						: "command.summy-reliquary-apocrypha.fear.players.off"), true);
		return 1;
	}

	private static Component onOff(boolean value) {
		return Component.translatable(value ? "options.on" : "options.off");
	}
}
