package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.cli.ArgumentParser;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.exception.MissingMemberException;
import dev.lvstrng.aidsfuscator.context.exception.MissingWorkspaceItemException;
import dev.lvstrng.aidsfuscator.log.Logger;

public class Main {
    private static final String BAD_ARGS = """
            Usage tutorial.
            You ran aidsfuscator with no arguments (or bad arguments). Aidsfuscator is a CLI tool, run the obfuscator using any of these args:
            These files have to be in the `workspace/` folder provided in the ZIP file. If the path contains spaces, add double quotes.
            \t`--config=`     (Required)
            \t`--exclusions=` (Optional)
            \t`--initOrder=`  (Optional)
            \t`--references=` (Optional)
            \t'--javaPath='   (Optional) (Specifies the Java Runtime class path to use to obfuscate the JAR file)
            """;

    public static void main(String[] args) {
        Logger.banner(AidsfuscatorInfo.type(), AidsfuscatorInfo.build());
        UpdateChecker.checkAndPrintUpdates();

        if (args.length == 0) {
            Logger.error(BAD_ARGS);
            return;
        }

        var context = Context.newInstance();
        new ArgumentParser(context).parse(args);
        try {
            context.run();
        } catch (MissingMemberException | MissingWorkspaceItemException e) {
            Logger.error(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
