package dev.lvstrng.aidsfuscator.transform.impl.data;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;

/**
 * Moves constants (final, static fields) into <clinit>.
 *
 * @author jonesdevelopment
 */
public class ConstantsFixTransformer extends Transformer {
  public ConstantsFixTransformer() {
    super("Fix Constants", "FixConstantsTransformer");
  }

  @Override
  public void transform(Context context) {
    for (JClass clazz : context.classes()) {
      if (Exclusions.FIX_CONSTANTS.excluded(clazz)) {
        continue;
      }

      for (JField field : clazz.fields()) {
        if (field.value() == null) {
          continue;
        }

        if (field.isVirtual()) {
          continue;
        }

        if (Exclusions.FIX_CONSTANTS.excluded(field)) {
          continue;
        }

        // First, move the initialization to `<clinit>`. Then, delete the value.
        InsnBuilder builder = new InsnBuilder()
            ._const(field.value())
            .field(PUTSTATIC, clazz.name(), field.name(), field.desc());
        clazz.findOrCreateClinit().insertSafe(builder.result());
        field.setValue(null);

        markChange();
      }
    }
  }
}
