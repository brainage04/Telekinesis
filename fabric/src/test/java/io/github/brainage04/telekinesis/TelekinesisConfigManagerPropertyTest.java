package io.github.brainage04.telekinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonParser;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Any sequence of commands, reloads and edits to {@code telekinesis.json} keeps the server-wide
 * setting and the file where the documented rule puts them: a file that fails to load keeps the
 * current setting and is not overwritten until a later reload succeeds or finds the file missing.
 */
class TelekinesisConfigManagerPropertyTest {
    sealed interface Step {}

    /** {@code /telekinesis config enabled <enabled>}. */
    record SetEnabled(boolean enabled) implements Step {}

    /** {@code /telekinesis config reload}. */
    record Reload() implements Step {}

    /** Someone edits the file by hand; {@code enabled} is empty for a file that does not load. */
    record Edit(String content, Optional<Boolean> enabled) implements Step {}

    record Delete() implements Step {}

    private static final List<Edit> EDITS =
            List.of(
                    new Edit("{\"enabled\":true}", Optional.of(true)),
                    new Edit("{\"enabled\":false}", Optional.of(false)),
                    new Edit("{\n  \"enabled\": false\n}\n", Optional.of(false)),
                    new Edit("{}", Optional.of(true)),
                    new Edit("{broken", Optional.empty()),
                    new Edit("null", Optional.empty()),
                    new Edit("", Optional.empty()),
                    new Edit("[]", Optional.empty()));

    @Provide
    Arbitrary<Edit> edits() {
        return Arbitraries.of(EDITS);
    }

    @Provide
    Arbitrary<Optional<Edit>> startingFiles() {
        return edits().optional();
    }

    @Provide
    Arbitrary<List<Step>> steps() {
        Arbitrary<Step> step =
                Arbitraries.oneOf(
                        Arbitraries.of(true, false).map(SetEnabled::new),
                        Arbitraries.just(new Reload()),
                        edits().map(edit -> (Step) edit),
                        Arbitraries.just(new Delete()));
        return step.list().ofMaxSize(25);
    }

    @Property(tries = 300)
    void settingAndFileFollowTheFailedLoadRule(
            @ForAll("startingFiles") Optional<Edit> startingFile, @ForAll("steps") List<Step> steps)
            throws IOException {
        Path directory = Files.createTempDirectory("telekinesis-config");
        try {
            Path file = directory.resolve("telekinesis.json");
            // The setting is static and a failed initialize keeps it, so each try starts from the
            // default setting, as a fresh server does.
            Files.writeString(file, "{\"enabled\":true}");
            TelekinesisConfigManager.initialize(directory);
            Model model = new Model();
            model.enabled = true;
            if (startingFile.isPresent()) {
                Files.writeString(file, startingFile.get().content());
                model.file = startingFile;
                model.reload();
            } else {
                Files.delete(file);
                model.written(true);
            }
            TelekinesisConfigManager.initialize(directory);
            check(model, file, "after initialize");

            for (Step step : steps) {
                switch (step) {
                    case SetEnabled set -> {
                        boolean saved = TelekinesisConfigManager.setEnabled(set.enabled());
                        model.enabled = set.enabled();
                        assertEquals(!model.loadFailed, saved, "saved " + step);
                        if (!model.loadFailed) {
                            model.written(set.enabled());
                        }
                    }
                    case Reload reload -> {
                        boolean loaded = TelekinesisConfigManager.reload();
                        boolean expected = model.reload();
                        assertEquals(expected, loaded, "reloaded");
                    }
                    case Edit edit -> {
                        Files.writeString(file, edit.content());
                        model.file = Optional.of(edit);
                    }
                    case Delete delete -> {
                        Files.deleteIfExists(file);
                        model.file = Optional.empty();
                    }
                }
                check(model, file, "after " + step + " in " + steps);
            }
        } finally {
            try (Stream<Path> paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void check(Model model, Path file, String when) throws IOException {
        assertEquals(model.enabled, TelekinesisConfigManager.config().enabled(), "setting " + when);
        if (model.file.isEmpty()) {
            assertFalse(Files.exists(file), "file exists " + when);
            return;
        }
        Edit expected = model.file.get();
        String content = Files.readString(file);
        if (expected.enabled().isEmpty()) {
            assertEquals(expected.content(), content, "file overwritten " + when);
            return;
        }
        boolean written =
                !JsonParser.parseString(content).getAsJsonObject().has("enabled")
                        || JsonParser.parseString(content)
                                .getAsJsonObject()
                                .get("enabled")
                                .getAsBoolean();
        assertEquals(expected.enabled().get(), written, "file " + when + ": " + content);
    }

    private static final class Model {
        boolean enabled;

        /** Absent when there is no file. */
        Optional<Edit> file = Optional.empty();

        boolean loadFailed;

        void written(boolean value) {
            file = Optional.of(new Edit(null, Optional.of(value)));
        }

        boolean reload() {
            if (file.isEmpty()) {
                loadFailed = false;
                return false;
            }
            Optional<Boolean> loaded = file.get().enabled();
            loadFailed = loaded.isEmpty();
            loaded.ifPresent(value -> enabled = value);
            return loaded.isPresent();
        }
    }
}
