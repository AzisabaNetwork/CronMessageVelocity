package net.azisaba.cronmessagevelocity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

public enum CondOp implements BiPredicate<@NotNull String, @NotNull String> {
    CONTAINS(String::contains, "contain"),
    EQUALS(String::equals, "==", "eq"),
    EQUALS_IGNORE_CASE(String::equalsIgnoreCase, "equalsIgnoreCase"),
    NOT_EQUALS((variable, value) -> !variable.equals(value), "!=", "notEquals"),
    NOT_EQUALS_IGNORE_CASE((variable, value) -> !variable.equalsIgnoreCase(value), "notEqualsIgnoreCase"),
    ;

    private static final Map<String, CondOp> MAP = new HashMap<>();
    private final BiPredicate<@NotNull String, @NotNull String> predicate;
    private final String[] alias;

    CondOp(@NotNull BiPredicate<@NotNull String, @NotNull String> predicate, @NotNull String @NotNull ... alias) {
        this.predicate = predicate;
        this.alias = alias;
    }

    @Override
    public boolean test(@NotNull String variable, @NotNull String value) {
        return predicate.test(variable, value);
    }

    public static @Nullable CondOp getOp(@NotNull String name) {
        return MAP.get(name.toLowerCase());
    }

    public static @NotNull Set<String> names() {
        return MAP.keySet();
    }

    private static void registerOp(@NotNull String name, @NotNull CondOp variable) {
        if (MAP.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("map already contains " + name.toLowerCase());
        }
        MAP.put(name.toLowerCase(), Objects.requireNonNull(variable));
    }

    static {
        for (CondOp value : values()) {
            registerOp(value.name(), value);
            for (String alias : value.alias) {
                registerOp(alias, value);
            }
        }
    }
}
