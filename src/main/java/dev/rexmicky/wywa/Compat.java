package dev.rexmicky.wywa;

import org.bukkit.attribute.Attribute;

/**
 * Cross-version API shim.
 *
 * Bukkit renamed the attribute constants in 1.21.3 (GENERIC_MAX_HEALTH -> MAX_HEALTH)
 * and changed Attribute from an enum to an interface. Referencing either name directly
 * makes the jar throw NoSuchFieldError on the other side of that split, so we resolve
 * them reflectively once at class-load and use whichever the running server has.
 */
public final class Compat {

    public static final Attribute MAX_HEALTH = resolve("MAX_HEALTH", "GENERIC_MAX_HEALTH");
    public static final Attribute ATTACK_DAMAGE = resolve("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
    public static final Attribute MOVEMENT_SPEED = resolve("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
    public static final Attribute KNOCKBACK_RESISTANCE = resolve("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE");

    private Compat() {}

    private static Attribute resolve(String... candidates) {
        for (String name : candidates) {
            try {
                Object value = Attribute.class.getField(name).get(null);
                if (value instanceof Attribute attribute) return attribute;
            } catch (Throwable ignored) {
                // try the next spelling
            }
        }
        return null;   // callers already null-check before use
    }
}
