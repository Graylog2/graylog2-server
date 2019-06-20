/* global ace */

let builtinFunctions = '';

export class PipelineHighlightRules extends ace.require('ace/mode/text_highlight_rules').TextHighlightRules {
  constructor() {
    super();
    const keywords = 'let|when|then|rule|end';

    const builtinConstants = 'and|or|not|during';

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
  }
}

export default class PipelineRulesMode extends ace.require('ace/mode/text').Mode {
  constructor(args) {
    super(args);

    builtinFunctions = args;
    this.HighlightRules = PipelineHighlightRules;
  }
}
