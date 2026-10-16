package dev.lvstrng.aidsfuscator.reference.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.StringFilter;
import dev.lvstrng.aidsfuscator.reference.IReferenceCandidate;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

public class MethodReferenceCandidate implements IReferenceCandidate {
    private final Context context;
    private final StringFilter filter;
    private final String filterString;

    public MethodReferenceCandidate(Context context, String filter) {
        this.context = context;
        this.filter = new StringFilter(filter);
        this.filterString = filter;
    }

    @Override
    public boolean test(String owner, String name, String desc) {
        var clazz = context.forName(owner);
        var method = clazz.findMethodFull(context, name, desc);
        if(method == null) {
            return filter.test(MemberUtils.fullMethod(clazz.originalName(), name, desc));
        } else {
            return filter.test(method.fullOriginalName());
        }
    }

    @Override
    public String getFilterString() {
        return filterString;
    }
}
