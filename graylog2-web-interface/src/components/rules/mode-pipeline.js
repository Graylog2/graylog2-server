// Used https://github.com/ajaxorg/ace-builds/blob/master/src-noconflict/mode-java.js as template
/* eslint-disable */

ace.define('ace/mode/pipeline_highlight_rules', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/doc_comment_highlight_rules', 'ace/mode/text_highlight_rules'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const { TextHighlightRules } = require('./text_highlight_rules');

  const PipelineHighlightRules = function () {
    const keywords = 'let|when|then|rule|end';

    const builtinConstants = 'and|or|not|during';

    const builtinFunctions = window.pipelineRulesFunctions; // set in SourceCodeEditor#componentDidMount

    const keywordMapper = this.createKeywordMapper({
      'variable.language': 'stage',
      'support.type': '$message',
      'support.function': builtinFunctions,
      keyword: keywords,
      'constant.language': builtinConstants,
    }, 'identifier');

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

  oop.inherits(PipelineHighlightRules, TextHighlightRules);

  exports.PipelineHighlightRules = PipelineHighlightRules;
});

ace.define('ace/mode/pipeline', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/text', 'ace/mode/pipeline_highlight_rules'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const TextMode = require('./text').Mode;
  const { PipelineHighlightRules } = require('./pipeline_highlight_rules');
  const Mode = function () {
    TextMode.call(this);
    this.HighlightRules = PipelineHighlightRules;
  };
  oop.inherits(Mode, TextMode);

  (function () {
    this.createWorker = function (session) {
      return null;
    };

    this.$id = 'ace/mode/pipeline';
  }).call(Mode.prototype);

  exports.Mode = Mode;
}); (function () {
  ace.require(['ace/mode/pipeline'], (m) => {
    if (typeof module === 'object' && typeof exports === 'object' && module) {
      module.exports = m;
    }
  });
}());
