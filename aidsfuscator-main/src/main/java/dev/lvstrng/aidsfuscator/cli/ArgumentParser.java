package dev.lvstrng.aidsfuscator.cli;

import dev.lvstrng.aidsfuscator.cli.impl.*;
import dev.lvstrng.aidsfuscator.context.Context;

import java.util.Arrays;
import java.util.List;

public class ArgumentParser {
    private final Context context;
    private final List<ArgumentHandler> handlers = List.of(
            new ConfigArgumentHandler(),
            new ExclusionArgumentHandler(),
            new ReferenceArgumentHandler(),
            new JavaPathArgumentHandler(),
            new InitOrderArgumentHandler()
    );

    public ArgumentParser(Context context) {
        this.context = context;
    }

    public void parse(String... args) {
        Arrays.stream(args).forEach(arg -> {
            var handlerOpt = handlers.stream().filter(e -> arg.startsWith(e.prefix())).findAny();

            if(handlerOpt.isPresent()) {
                var handler = handlerOpt.get();
                handler.provideContext(context).run(arg.substring(handler.prefix().length()));
            }
        });
    }
}
