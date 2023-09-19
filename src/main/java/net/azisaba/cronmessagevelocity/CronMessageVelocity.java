package net.azisaba.cronmessagevelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Plugin(id = "cronmessagevelocity", name = "CronMessageVelocity", version = "1.0", authors = "Azisaba Network")
public class CronMessageVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final String timeZone;
    private final Map<LocalTime, List<Function<@NotNull VariableContext, @Nullable String>>> cron = new ConcurrentHashMap<>();
    private LocalTime previousTime = null;

    @Inject
    public CronMessageVelocity(@NotNull ProxyServer server, @NotNull Logger logger, @NotNull @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        Path configPath = dataDirectory.resolve("config.yml");
        String allAvailableVariableNames = String.join(", ", CondVariable.names());
        String allAvailableOpNames = String.join(", ", CondOp.names());
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectory(dataDirectory);
            }
            Files.write(
                    dataDirectory.resolve("README.yml"),
                    Arrays.asList(
                            "available-variables:",
                            "- " + String.join("\n- ", CondVariable.names()),
                            "available-ops:",
                            "- " + String.join("\n- ", CondOp.names())
                    ),
                    StandardOpenOption.CREATE
            );
            if (!Files.exists(configPath)) {
                Files.write(
                        configPath,
                        Arrays.asList(
                                "time-zone: UTC",
                                "cron:",
                                "  \"20:00:00\":",
                                "  - text: \"&atest message\"",
                                "  - text: \"&aconditional test message\"",
                                "    condition: pod_name contains 1"
                        ),
                        StandardOpenOption.CREATE
                );
            }
        } catch (IOException ex) {
            logger.warn("Failed to write config.yml", ex);
        }
        try {
            ConfigurationNode node = YAMLConfigurationLoader.builder().setPath(configPath).build().load();
            this.timeZone = node.getNode("time-zone").getString("UTC");
            ConfigurationNode cronNode = node.getNode("cron");
            for (Object o : cronNode.getChildrenMap().keySet()) {
                int[] intArray = Arrays.stream(o.toString().split(":")).mapToInt(Integer::parseInt).toArray();
                if (intArray.length != 3) {
                    logger.warn("Malformed time: {}", o);
                    continue;
                }
                LocalTime time = LocalTime.of(intArray[0], intArray[1], intArray[2]);
                List<Function<VariableContext, String>> list = new ArrayList<>();
                for (ConfigurationNode childrenNode : cronNode.getNode(o).getChildrenList()) {
                    String text = Objects.requireNonNull(childrenNode.getNode("text").getString(), "text is missing");
                    String condition = childrenNode.getNode("condition").getString();
                    if (condition != null) {
                        String[] split = condition.split(" ");
                        CondVariable variable = CondVariable.getVariable(split[0]);
                        if (variable == null) {
                            logger.warn("Invalid variable: {} (available names: {})", split[0], allAvailableVariableNames);
                            continue;
                        }
                        CondOp op = CondOp.getOp(split[1]);
                        if (op == null) {
                            logger.warn("Invalid op: {} (available names: {})", split[2], allAvailableOpNames);
                            continue;
                        }
                        String value = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                        list.add(ctx -> op.test(variable.apply(ctx), value) ? text : null);
                    } else {
                        list.add(ctx -> text);
                    }
                }
                if (!list.isEmpty()) {
                    cron.put(time, list);
                }
            }
            logger.info("{} cron times loaded", cron.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        server.getScheduler().buildTask(this, () -> {
            LocalTime now = LocalTime.now(ZoneId.of(timeZone));
            LocalTime time = null;
            for (LocalTime localTime : cron.keySet()) {
                if (now.getHour() == localTime.getHour() && now.getMinute() == localTime.getMinute() && now.getSecond() == localTime.getSecond()) {
                    time = localTime;
                    break;
                }
            }
            if (time == null) return;
            if (time == previousTime) return;
            previousTime = time;
            var list = Objects.requireNonNull(cron.get(time));
            for (Player player : server.getAllPlayers()) {
                ServerConnection connection = player.getCurrentServer().orElse(null);
                VariableContext ctx = new VariableContext(server, player, connection);
                for (Function<VariableContext, String> function : list) {
                    try {
                        var text = function.apply(ctx);
                        if (text != null) {
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
                        }
                    } catch (RuntimeException ex) {
                        logger.error("Error evaluating condition for {}", player.getUsername(), ex);
                    }
                }
            }
        }).repeat(500, TimeUnit.MILLISECONDS).schedule();
    }
}
