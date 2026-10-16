package dev.lvstrng.aidsfuscator.analysis.interpreter;

import org.objectweb.asm.tree.analysis.Frame;

public class FrameString {
    public static String generate(Frame<SimpleValue> frame) {
        var sb = new StringBuilder("L{");
        for (int i = 0; i < frame.getLocals(); i++) {
            sb.append(i).append(":").append(frame.getLocal(i)).append(",");
        }
        sb.append("}S{");
        for (int i = 0; i < frame.getStackSize(); i++) {
            sb.append(i).append(":").append(frame.getStack(i)).append(",");
        }

        sb.append("}");
        return sb.toString();
    }
}
