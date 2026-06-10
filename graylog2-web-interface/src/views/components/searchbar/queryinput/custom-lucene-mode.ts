/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

declare const ace: {
  define: (name: string, deps: Array<string>, factory: (...args: Array<unknown>) => void) => void;
  require: (deps: Array<string>, callback: (m: unknown) => void) => void;
};

ace.define(
  'ace/mode/lucene_highlight_rules',
  ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/text_highlight_rules'],
  (require: (path: string) => any, exports: Record<string, unknown>) => {
    const oop = require('../lib/oop');
    const { TextHighlightRules } = require('./text_highlight_rules');

    // eslint-disable-next-line func-names
    const LuceneHighlightRules = function (this: any) {
      this.$rules = {
        start: [
          {
            token: 'constant.language.escape',
            regex: /\\[-+&|!(){}[\]^"~*?:\\]/,
          },
          {
            token: 'constant.character.negation',
            regex: '\\-',
          },
          {
            token: 'constant.character.interro',
            regex: '\\?',
          },
          {
            token: 'constant.character.required',
            regex: '\\+',
          },
          {
            token: 'constant.character.asterisk',
            regex: '\\*',
          },
          {
            token: 'constant.character.proximity',
            regex: '~(?:0\\.[0-9]+|[0-9]+)?',
          },
          {
            token: 'keyword.operator',
            regex: '(AND|OR|NOT|TO)\\b',
          },
          {
            token: (value: string) => [
              {
                type: 'paren.lparen',
                value: value,
              },
            ],
            regex: '[\\(\\{\\[]',
          },
          {
            token: (value: string) => [
              {
                type: 'paren.rparen',
                value: value,
              },
            ],
            regex: '[\\)\\}\\]]',
          },
          {
            token: 'keyword.operator',
            regex: /[><=^]/,
          },
          {
            token: 'constant.numeric',
            regex: /\d[\d.-]*/,
          },
          {
            token: 'string',
            regex: /"(?:\\"|[^"])*"/,
          },
          {
            token: 'keyword',
            regex: /(?:\\.|[^\s\-+&|!(){}[\]^"~*?:\\])+:/,
            next: 'maybeRegex',
          },
          {
            token: 'term',
            regex: /[\w\\/.]+/,
          },
          {
            token: 'text',
            regex: /\s+/,
          },
        ],
        maybeRegex: [
          {
            token: 'text',
            regex: /\s+/,
          },
          {
            token: 'string.regexp.start',
            regex: '/',
            next: 'regex',
          },
          {
            regex: '',
            next: 'start',
          },
        ],
        regex: [
          {
            token: 'regexp.keyword.operator',
            regex: '\\\\(?:u[\\da-fA-F]{4}|x[\\da-fA-F]{2}|.)',
          },
          {
            token: 'string.regexp.end',
            regex: '/[sxngimy]*',
            next: 'start',
          },
          {
            token: 'invalid',
            regex: /\{\d+\b,?\d*\}[+*]|[+*$^?][+*]|[$^][?]|\?{3,}/,
          },
          {
            token: 'constant.language.escape',
            regex: /\(\?[:=!]|\)|\{\d+\b,?\d*\}|[+*]\?|[()$^+*?.]/,
          },
          {
            token: 'constant.language.escape',
            // eslint-disable-next-line no-useless-escape
            regex: '<\d+-\d+>|[~&@]',
          },
          {
            token: 'constant.language.delimiter',
            regex: /\|/,
          },
          {
            token: 'constant.language.escape',
            regex: /\[\^?/,
            next: 'regex_character_class',
          },
          {
            token: 'empty',
            regex: '$',
            next: 'start',
          },
          {
            defaultToken: 'string.regexp',
          },
        ],
        regex_character_class: [
          {
            token: 'regexp.charclass.keyword.operator',
            regex: '\\\\(?:u[\\da-fA-F]{4}|x[\\da-fA-F]{2}|.)',
          },
          {
            token: 'constant.language.escape',
            regex: ']',
            next: 'regex',
          },
          {
            token: 'constant.language.escape',
            regex: '-',
          },
          {
            token: 'empty',
            regex: '$',
            next: 'start',
          },
          {
            defaultToken: 'string.regexp.charachterclass',
          },
        ],
      };
    };

    oop.inherits(LuceneHighlightRules, TextHighlightRules);

    // eslint-disable-next-line no-param-reassign
    exports.LuceneHighlightRules = LuceneHighlightRules;
  },
);

ace.define(
  'ace/mode/lucene',
  ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/text', 'ace/mode/lucene_highlight_rules'],
  (require: (path: string) => any, exports: Record<string, unknown>) => {
    const oop = require('../lib/oop');
    const TextMode = require('./text').Mode;
    const { LuceneHighlightRules } = require('./lucene_highlight_rules');

    // eslint-disable-next-line func-names
    const Mode = function (this: any) {
      this.HighlightRules = LuceneHighlightRules;
      this.$behaviour = this.$defaultBehaviour;
    };

    oop.inherits(Mode, TextMode);

    // eslint-disable-next-line func-names
    (function (this: any) {
      this.$id = 'ace/mode/lucene';
    }).call(Mode.prototype);

    // eslint-disable-next-line no-param-reassign
    exports.Mode = Mode;
  },
);

// eslint-disable-next-line func-names
(function () {
  ace.require(['ace/mode/lucene'], (m: unknown) => {
    if (typeof module === 'object' && typeof exports === 'object' && module) {
      module.exports = m;
    }
  });
})();
