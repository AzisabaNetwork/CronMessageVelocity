package net.azisaba.cronmessagevelocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VariableContext(@NotNull ProxyServer server, @NotNull Player player, @Nullable ServerConnection connection) {
}
