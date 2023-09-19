package net.azisaba.cronmessagevelocity;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public enum CondVariable implements Function<VariableContext, String> {
    POD_NAME(ctx -> System.getenv("POD_NAME"), "podName", "proxy_id", "proxyId"),
    NODE_NAME(ctx -> System.getenv("NODE_NAME"), "nodeName", "proxy_host", "proxyHost"),
    PLAYER_NAME(ctx -> ctx.player().getUsername(), "playerName"),
    CLIENT_BRAND(ctx -> ctx.player().getClientBrand(), "clientBrand"),
    SERVER_NAME(ctx -> Optional.ofNullable(ctx.connection()).map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null), "serverName"),
    ;

    private static final Map<String, CondVariable> MAP = new HashMap<>();
    private final Function<VariableContext, ?> supplier;
    private final String[] alias;

    CondVariable(Function<VariableContext, ?> supplier, String... alias) {
        this.supplier = supplier;
        this.alias = alias;
    }

    @Override
    public String apply(VariableContext variableContext) {
        return String.valueOf(supplier.apply(variableContext));
    }

    public static @Nullable CondVariable getVariable(@NotNull String name) {
        return MAP.get(name.toLowerCase());
    }

    public static @NotNull Set<String> names() {
        return MAP.keySet();
    }

    private static void registerVariable(@NotNull String name, @NotNull CondVariable variable) {
        if (MAP.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("map already contains " + name.toLowerCase());
        }
        MAP.put(name.toLowerCase(), Objects.requireNonNull(variable));
    }

    static {
        for (CondVariable value : values()) {
            registerVariable(value.name(), value);
            for (String alias : value.alias) {
                registerVariable(alias, value);
            }
        }
    }
}
