package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.NamedOpcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.*;

import java.util.HashMap;
import java.util.Map;

public class SourceInstructionTestTransformer extends Transformer {
    public SourceInstructionTestTransformer() {
        super("Source Instruction Test", "sourceInsnTest");
    }

    @Override
    public void transform(Context context) {
        /*for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var simpleFrames = method.frames(context);
                var sourceFrames = analyzeSource(method);
                if(simpleFrames == null) {
                    Logger.error("Simple Frames null in %s", method.fullName());

                    continue;
                } else if (sourceFrames == null) {
                    Logger.error("Source Frames null in %s", method.fullName());
                    continue;
                }

                b:
                for(var insn : method.insns()) {
                    if(insn.getOpcode() == -1)
                        continue;

                    var simpleFrame = simpleFrames.get(insn);
                    if(simpleFrame == null || simpleFrame.getStackSize() <= 0)
                        continue;

                    var sourceFrame = sourceFrames.get(insn);
                    for(int i = 0; i < simpleFrame.getStackSize(); i++) {
                        var simpleValue = simpleFrame.getStack(i);
                        var sourceValue = sourceFrame.getStack(i);

                        if(simpleValue.producers().size() != sourceValue.insns.size()) {
                            Logger.error("{%s} Source instruction mismatch @%s[%s] (%s). Simple: %s; Source: %s", method.fullName(), method.idx(insn), i, NamedOpcodes.map(insn.getOpcode()), simpleValue.producers().size(), sourceValue.insns.size());
                            Logger.error("Simple:");
                            for(var ian : simpleValue.producers()) {
                                Logger.error("\t%s {%s; %s}", NamedOpcodes.map(ian.getOpcode()), method.idx(ian), ian.hashCode());
                            }
                            Logger.error("Source:");
                            for(var ian : sourceValue.insns) {
                                Logger.error("\t%s {%s; %s}", NamedOpcodes.map(ian.getOpcode()), method.idx(ian), ian.hashCode());
                            }
                            Logger.error("");
                            markChange();
                            break b;
                        }
                    }
                }
            }
        }*/
    }

    private Map<AbstractInsnNode, Frame<SourceValue>> analyzeSource(JMethod method) {
        try {
            var frameArr = new Analyzer<>(new SourceInterpreter()).analyzeAndComputeMaxs(method.owner().name(), method.core());
            var map = new HashMap<AbstractInsnNode, Frame<SourceValue>>();

            for(int i = 0; i < method.insns().size(); i++) {
                map.put(method.insns().get(i), frameArr[i]);
            }

            return map;
        } catch (AnalyzerException _) {
            return null;
        }
    }
}
