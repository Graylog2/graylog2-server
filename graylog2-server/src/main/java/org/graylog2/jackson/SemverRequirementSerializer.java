/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.vdurmont.semver4j.Range;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.graylog2.shared.SuppressForbidden;

import java.io.IOException;
import java.lang.reflect.Field;

public class SemverRequirementSerializer extends StdSerializer<Requirement> {
    public SemverRequirementSerializer() {
        super(Requirement.class);
    }

    @Override
    public void serialize(final Requirement value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        // FIXME: Dirty hack until https://github.com/vdurmont/semver4j/pull/33 has been merged and released.
        final PrettyRequirement prettyRequirement = PrettyRequirement.valueOf(value);
        final String s = prettyRequirement.toString();
        gen.writeString(s);
    }

    static class PrettyRange extends Range {
        PrettyRange(Semver version, RangeOperator op) {
            super(version, op);
        }

        @Override
        public String toString() {
            switch (op) {
                case EQ:
                    return "=" + version;
                case LT:
                    return "<" + version;
                case LTE:
                    return "<=" + version;
                case GT:
                    return ">" + version;
                case GTE:
                    return ">=" + version;
                default:
                    throw new IllegalStateException("Unknown range operator: " + op);
            }
        }

        @SuppressForbidden("Reflection necessary for dirty hack")
        public static PrettyRange valueOf(Range range) {
            if (range == null) {
                return null;
            }

            try {
                final Field versionField = Range.class.getDeclaredField("version");
                versionField.setAccessible(true);
                final Semver version = (Semver) versionField.get(range);
                final Field opField = Range.class.getDeclaredField("op");
                opField.setAccessible(true);
                final RangeOperator op = (RangeOperator) opField.get(range);

                return new PrettyRange(version, op);
            } catch (Exception e) {
                throw new IllegalArgumentException("Couldn't get version and operator from Range", e);
            }
        }
    }

    static class PrettyRequirement extends Requirement {
        PrettyRequirement(Range range, Requirement req1, RequirementOperator op, Requirement req2) {
            super(range, req1, op, req2);
        }

        @Override
        public String toString() {
            if (this.range != null) {
                return this.range.toString();
            }
            return this.req1 + (this.op == RequirementOperator.OR ? " || " : " ") + this.req2;
        }

        @SuppressForbidden("Reflection necessary for dirty hack")
        public static PrettyRequirement valueOf(Requirement requirement) {
            if (requirement == null) {
                return null;
            }

            try {
                final Field rangeField = Requirement.class.getDeclaredField("range");
                rangeField.setAccessible(true);
                final Range range = PrettyRange.valueOf((Range) rangeField.get(requirement));
                final Field req1Field = Requirement.class.getDeclaredField("req1");
                req1Field.setAccessible(true);
                final Requirement req1 = PrettyRequirement.valueOf((Requirement) req1Field.get(requirement));
                final Field opField = Requirement.class.getDeclaredField("op");
                opField.setAccessible(true);
                final RequirementOperator op = (RequirementOperator) opField.get(requirement);
                final Field req2Field = Requirement.class.getDeclaredField("req2");
                req2Field.setAccessible(true);
                final Requirement req2 = PrettyRequirement.valueOf((Requirement) req2Field.get(requirement));

                return new PrettyRequirement(range, req1, op, req2);
            } catch (Exception e) {
                throw new IllegalArgumentException("Couldn't get internal fields from Requirement", e);
            }
        }
    }
}
