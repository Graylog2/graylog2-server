import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import 'brace/mode/java';

const loadFunctions = () => {
  const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.functions().url);

  return fetch('GET', url).then(response => response.map(res => res.name).join('|'));
};
const keywords = 'let|rule|match|pipeline';
const operators = 'and|or|not';
const builtinConstants = 'all|either|during|when|then|end';

let builtinFunctions = '';

export class PipelineHighlightRules extends window.ace.acequire('ace/mode/text_highlight_rules').TextHighlightRules {
  constructor() {
    super();

    const keywordMapper = this.createKeywordMapper(
      {
        'constant.language': builtinConstants,
        keyword: keywords,
        'support.function': builtinFunctions,
        'support.type': '$message',
        'variable.language': 'stage',
        'language.support.class': operators,
      },
      'identifier',
      true,
    );

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

export default class PipelineRulesMode extends window.ace.acequire('ace/mode/java').Mode {
  constructor() {
    super();

    return loadFunctions().then((res) => {
      builtinFunctions = res;
      this.HighlightRules = PipelineHighlightRules;

      return this;
    });
  }
}
