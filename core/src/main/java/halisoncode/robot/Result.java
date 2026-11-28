package halisoncode.robot;

import java.util.List;

/**
 * Outcome of one robot command: {@code ok} with a value, or an error code from the protocol (architecture section 3).
 * An ok result may carry a message too: why an action did less than asked (no blocks left), shown in the editor's log.
 * A message the robot itself says (issue #447) also has a translation {@code key} and its {@code args}, so the editor
 * can show it in its own language; {@code message} stays the English text, for an editor that does not know the key.
 */
public record Result(boolean ok, Object value, Code code, String message, String key, List<String> args) {
    public enum Code { BAD_ARGS, NO_PERMISSION, RATE_LIMITED, PROTECTED, UNLOADED, OUT_OF_RANGE, NO_ITEMS, STOPPED, VERSION }

    /** A sentence with a translation key: {@code english} is the key's English text with {@code args} filled in. */
    public record Text(String key, List<String> args, String english) {
    }

    public static Result ok(Object value) {
        return new Result(true, value, null, null, null, null);
    }

    /** Ok, with a sentence for the player on why the action did less than asked. */
    public static Result ok(Object value, String message) {
        return new Result(true, value, null, message, null, null);
    }

    /** As {@link #ok(Object, String)}, with a sentence the editor can translate. */
    public static Result ok(Object value, Text text) {
        return new Result(true, value, null, text.english(), text.key(), text.args());
    }

    public static Result error(Code code, String message) {
        return new Result(false, null, code, message, null, null);
    }

    /** As {@link #error(Code, String)}, with a sentence the editor can translate. */
    public static Result error(Code code, Text text) {
        return new Result(false, null, code, text.english(), text.key(), text.args());
    }

    @Override
    public String toString() {
        return ok ? "ok " + value + (message == null ? "" : " (" + message + ")") : code + ": " + message;
    }
}
