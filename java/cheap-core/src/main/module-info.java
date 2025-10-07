module net.netbeing.cheap.core {
    exports net.netbeing.cheap.model;
    exports net.netbeing.cheap.impl.basic;
    exports net.netbeing.cheap.impl.reflect;
    exports net.netbeing.cheap.util;
    exports net.netbeing.cheap.util.reflect;

    requires transitive org.apache.commons.math3;
    requires com.google.common;
    requires static org.jetbrains.annotations;
    requires java.desktop;
}
