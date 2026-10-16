package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.utils.NamedOpcodes;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;

public class FrameTest extends Transformer {
    public FrameTest() {
        super("Frame Test", "frameTest");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                Logger.info(method.fullName());
                var frames = method.frames(context);
                if(frames == null)
                    return;

                var lbls = new ArrayList<LabelNode>();
                for(var insn : method.insns()) {
                    var frame = frames.get(insn);
                    if(frame == null)
                        continue;

                    if(insn instanceof LabelNode l) {
                        int idx;
                        if(lbls.contains(l)) {
                            idx = lbls.indexOf(l);
                        } else {
                            idx = lbls.size();
                            lbls.add(l);
                        }

                        System.out.println("L" + idx + ":");
                        continue;
                    }

                    var suffix = "";
                    if(method.name().equals("<init>")) {
                        suffix = " initializedThis: " + frame.getLocal(0).isInitializedThis();
                    }
                    System.out.println("\t" + NamedOpcodes.map(insn.getOpcode()) + suffix);
                }
                System.out.println();
            }
        }
    }
}
