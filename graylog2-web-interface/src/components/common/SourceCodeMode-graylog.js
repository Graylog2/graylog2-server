import * as ace from 'ace-builds/src-min-noconflict/ace.js';

/* eslint-disable */
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const loadFunctions = () => {
  const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.functions().url);

  return fetch('GET', url).then(response => response.map(res => res.name).join('|'));
};
const keywords = 'let|rule|match|pipeline';
const operators = 'and|or|not';
const builtinConstants = 'all|either|during|when|then|end';

let builtinFunctions = '';

ace.define("ace/mode/graylog_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/java_highlight_rules"], function(require, exports, module) {
  "use strict";

  var oop = require("../lib/oop");
  var JavaHighlightRules = require("./java_highlight_rules").JavaHighlightRules;

  var GraylogHighlightRules = function() {
    var keywordMapper = this.createKeywordMapper({
      'constant.language': builtinConstants,
      keyword: keywords,
      'support.function': builtinFunctions,
      'support.type': '$message',
      'variable.language': 'stage',
      'language.support.class': operators,
    }, "identifier", true);

    this.$rules = {
      start: [
        {
          token: 'comment',
          regex: '\\/\\/.*$',
        },
        {
          token: 'comment', // multi line comment
          regex: '\\/\\*',
          next: 'comment',
        },
        {
          token: 'string', // single line
          regex: '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]',
        },
        {
          token: 'string', // single line
          regex: "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']",
        },
        {
          token: 'constant.numeric', // hex
          regex: /0(?:[xX][0-9a-fA-F][0-9a-fA-F_]*|[bB][01][01_]*)[LlSsDdFfYy]?\b/,
        },
        {
          token: 'constant.numeric', // float
          regex: /[+-]?\d[\d_]*(?:(?:\.[\d_]*)?(?:[eE][+-]?[\d_]+)?)?[LlSsDdFfYy]?\b/,
        },
        {
          token: 'constant.language.boolean',
          regex: '(?:true|false)\\b',
        },
        {
          token: 'language.support.class',
          regex: '&&',
        },
        {
          token: keywordMapper,
          regex: '[a-zA-Z_$][a-zA-Z0-9_$]*\\b',
        },
        {
          token: 'text',
          regex: '\\s+',
        },
      ],
      comment: [
        {
          token: 'comment', // closing comment
          regex: '\\*\\/',
          next: 'start',
        }, {
          defaultToken: 'comment',
        },
      ],
    };

    this.normalizeRules();
  };

  oop.inherits(GraylogHighlightRules, JavaHighlightRules);

  exports.GraylogHighlightRules = GraylogHighlightRules;
});

ace.define("ace/mode/graylog",["require","exports","module","ace/lib/oop","ace/mode/java","ace/mode/graylog_highlight_rules","ace/range"], function(require, exports, module) {
  "use strict";

  var oop = require("../lib/oop");
  var TextMode = require("./java").Mode;
  var GraylogHighlightRules = require("./graylog_highlight_rules").GraylogHighlightRules;
  var Range = require("../range").Range;

  var Mode = function() {
    this.HighlightRules = GraylogHighlightRules;
  };
  oop.inherits(Mode, TextMode);

  loadFunctions().then((res) => {
    builtinFunctions = res;

    (function() {
      this.lineCommentStart = "//";

      this.$id = "ace/mode/graylog";
    }).call(Mode.prototype);

    return this;
  });

  exports.Mode = Mode;
});
