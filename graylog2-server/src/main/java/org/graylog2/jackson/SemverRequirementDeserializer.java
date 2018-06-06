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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;

import java.io.IOException;

public class SemverRequirementDeserializer extends StdDeserializer<Requirement> {
    private final Semver.SemverType semverType;

    public SemverRequirementDeserializer() {
        this(Semver.SemverType.NPM);
    }

    public SemverRequirementDeserializer(Semver.SemverType semverType) {
        super(Requirement.class);
        this.semverType = semverType;
    }

    @Override
    public Requirement deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        switch (p.getCurrentTokenId()) {
            case JsonTokenId.ID_STRING:
                final String str = p.getText().trim();
                try {
                    return buildRequirement(str);
                } catch (SemverException e) {
                    ctxt.reportMappingException(e.getMessage());
                }
            default:
                throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, null);
        }
    }

    private Requirement buildRequirement(String s) {
        switch (semverType) {
            case STRICT:
                return Requirement.buildStrict(s);
            case LOOSE:
                return Requirement.buildLoose(s);
            case NPM:
                return Requirement.buildNPM(s);
            case COCOAPODS:
                return Requirement.buildCocoapods(s);
            case IVY:
                return Requirement.buildIvy(s);
            default:
                return null;
        }
    }
}