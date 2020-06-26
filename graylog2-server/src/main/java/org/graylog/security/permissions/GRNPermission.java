package org.graylog.security.permissions;

import com.google.auto.value.AutoValue;
import org.apache.shiro.authz.Permission;
import org.graylog2.utilities.GRN;

@AutoValue
public abstract class GRNPermission implements Permission {
    public abstract String type();

    public abstract GRN target();

    public static GRNPermission create(String type, GRN target) {
        return new AutoValue_GRNPermission(type, target);
    }

    @Override
    public boolean implies(Permission p) {
        // By default only supports comparisons with other GRNPermission
        if (!(p instanceof GRNPermission)) {
            return false;
        }
        GRNPermission other = (GRNPermission) p;

        return (other.type().equals(type()) && other.target().equals(target()));
    }
}
