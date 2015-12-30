package org.graylog.plugins.messageprocessor.parser;

import org.graylog.plugins.messageprocessor.parser.ParseError;
import org.graylog.plugins.messageprocessor.parser.RuleLangParser;

public class InvalidReference implements ParseError {
    private final RuleLangParser.IdentifierContext ctx;

    public InvalidReference(RuleLangParser.IdentifierContext ctx) {
        this.ctx = ctx;
    }
}
