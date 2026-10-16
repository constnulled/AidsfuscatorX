package dev.lvstrng.aidsfuscator.exclude.impl;

import java.util.regex.Pattern;

public class StringFilter {
    private final String string;
    private final Pattern pattern;

    public StringFilter(String pattern) {
        this.string = pattern;
        this.pattern = Pattern.compile(toRegex(pattern));
    }

    public boolean test(String s) {
        return pattern.matcher(s).matches();
    }

    /**
     * Turns a simple string with wildcards into a regex string (easier wildcard support xd)
     * @param p pattern
     * @return regex string
     */
    private String toRegex(String p) {
        var out = new StringBuilder("^");
        var wildcard = '*';

        for (int i = 0; i < p.length(); i++) {
            var c = p.charAt(i);

            if (c == wildcard) {
                out.append(".*");
                continue;
            }

            if ("\\.[]{}()+-^$|?".indexOf(c) >= 0)
                out.append('\\');

            out.append(c);
        }

        out.append("$");
        return out.toString();
    }

    public String string() {
        return string;
    }
}
