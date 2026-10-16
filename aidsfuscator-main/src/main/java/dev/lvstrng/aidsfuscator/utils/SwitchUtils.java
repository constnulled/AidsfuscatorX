package dev.lvstrng.aidsfuscator.utils;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SwitchUtils {
    public static LookupSwitchInsnNode sortSwitch(LookupSwitchInsnNode sw) {
        var entries = IntStream.range(0, sw.keys.size())
                .mapToObj(i -> Map.entry(sw.keys.get(i), sw.labels.get(i)))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        sw.keys.clear();
        sw.labels.clear();

        for (var entry : entries) {
            sw.keys.add(entry.getKey());
            sw.labels.add(entry.getValue());
        }

        return sw;
    }

    public static LookupSwitchInsnNode convertToLookup(TableSwitchInsnNode table) {
        var cases = tableToMap(table);
        return createLookup(table.dflt, cases);
    }

    public static LookupSwitchInsnNode createLookup(LabelNode dflt, Map<Integer, LabelNode> map) {
        var filtered = map.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return sortSwitch(new LookupSwitchInsnNode(dflt,
                filtered.keySet().stream().mapToInt(e -> e).toArray(),
                filtered.values().toArray(new LabelNode[0])
        ));
    }

    public static LookupSwitchInsnNode createLookupInvert(LabelNode dflt, Map<LabelNode, Integer> map) {
        return sortSwitch(new LookupSwitchInsnNode(dflt,
                map.values().stream().mapToInt(e -> e).toArray(),
                map.keySet().toArray(new LabelNode[0])
        ));
    }

    public static Map<Integer, LabelNode> tableToMap(TableSwitchInsnNode table) {
        var map = new HashMap<Integer, LabelNode>();

        for (int i = 0; i < table.labels.size(); i++) {
            int key = table.min + i;
            var label = table.labels.get(i);
            map.put(key, label);
        }

        return map;
    }
}
