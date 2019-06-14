// Used https://github.com/ajaxorg/ace-builds/blob/master/src-noconflict/mode-java.js as template
/* eslint-disable */
ace.define('ace/mode/doc_comment_highlight_rules', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/text_highlight_rules'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const { TextHighlightRules } = require('./text_highlight_rules');

  var DocCommentHighlightRules = function () {
    this.$rules = {
      start: [{
        token: 'comment.doc.tag',
        regex: '@[\\w\\d_]+', // TODO: fix email addresses
      },
      DocCommentHighlightRules.getTagRule(),
      {
        defaultToken: 'comment.doc',
        caseInsensitive: true,
      }],
    };
  };

  oop.inherits(DocCommentHighlightRules, TextHighlightRules);

  DocCommentHighlightRules.getTagRule = function (start) {
    return {
      token: 'comment.doc.tag.storage.type',
      regex: '\\b(?:TODO|FIXME|XXX|HACK)\\b',
    };
  };

  DocCommentHighlightRules.getStartRule = function (start) {
    return {
      token: 'comment.doc', // doc comment
      regex: '\\/\\*(?=\\*)',
      next: start,
    };
  };

  DocCommentHighlightRules.getEndRule = function (start) {
    return {
      token: 'comment.doc', // closing comment
      regex: '\\*\\/',
      next: start,
    };
  };


  exports.DocCommentHighlightRules = DocCommentHighlightRules;
});

ace.define('ace/mode/javascript_highlight_rules', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/doc_comment_highlight_rules', 'ace/mode/text_highlight_rules'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const { DocCommentHighlightRules } = require('./doc_comment_highlight_rules');
  const { TextHighlightRules } = require('./text_highlight_rules');
  const identifierRe = '[a-zA-Z\\$_\u00a1-\uffff][a-zA-Z\\d\\$_\u00a1-\uffff]*';

  const JavaScriptHighlightRules = function (options) {
    const keywordMapper = this.createKeywordMapper({
      'variable.language':
              'Array|Boolean|Date|Function|Iterator|Number|Object|RegExp|String|Proxy|' // Constructors
              + 'Namespace|QName|XML|XMLList|' // E4X
              + 'ArrayBuffer|Float32Array|Float64Array|Int16Array|Int32Array|Int8Array|'
              + 'Uint16Array|Uint32Array|Uint8Array|Uint8ClampedArray|'
              + 'Error|EvalError|InternalError|RangeError|ReferenceError|StopIteration|' // Errors
              + 'SyntaxError|TypeError|URIError|'
              + 'decodeURI|decodeURIComponent|encodeURI|encodeURIComponent|eval|isFinite|' // Non-constructor functions
              + 'isNaN|parseFloat|parseInt|'
              + 'JSON|Math|' // Other
              + 'this|arguments|prototype|window|document', // Pseudo
      keyword:
              'const|yield|import|get|set|async|await|'
              + 'break|case|catch|continue|default|delete|do|else|finally|for|function|'
              + 'if|in|of|instanceof|new|return|switch|throw|try|typeof|let|var|while|with|debugger|'
              + '__parent__|__count__|escape|unescape|with|__proto__|'
              + 'class|enum|extends|super|export|implements|private|public|interface|package|protected|static',
      'storage.type':
              'const|let|var|function',
      'constant.language':
              'null|Infinity|NaN|undefined',
      'support.function':
              'alert',
      'constant.language.boolean': 'true|false',
    }, 'identifier');
    const kwBeforeRe = 'case|do|else|finally|in|instanceof|return|throw|try|typeof|yield|void';

    const escapedRe = '\\\\(?:x[0-9a-fA-F]{2}|' // hex
          + 'u[0-9a-fA-F]{4}|' // unicode
          + 'u{[0-9a-fA-F]{1,6}}|' // es6 unicode
          + '[0-2][0-7]{0,2}|' // oct
          + '3[0-7][0-7]?|' // oct
          + '[4-7][0-7]?|' // oct
          + '.)';

    this.$rules = {
      no_regex: [
        DocCommentHighlightRules.getStartRule('doc-start'),
        comments('no_regex'),
        {
          token: 'string',
          regex: "'(?=.)",
          next: 'qstring',
        }, {
          token: 'string',
          regex: '"(?=.)',
          next: 'qqstring',
        }, {
          token: 'constant.numeric', // hexadecimal, octal and binary
          regex: /0(?:[xX][0-9a-fA-F]+|[oO][0-7]+|[bB][01]+)\b/,
        }, {
          token: 'constant.numeric', // decimal integers and floats
          regex: /(?:\d\d*(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+\b)?/,
        }, {
          token: [
            'storage.type', 'punctuation.operator', 'support.function',
            'punctuation.operator', 'entity.name.function', 'text', 'keyword.operator',
          ],
          regex: `(${identifierRe})(\\.)(prototype)(\\.)(${identifierRe})(\\s*)(=)`,
          next: 'function_arguments',
        }, {
          token: [
            'storage.type', 'punctuation.operator', 'entity.name.function', 'text',
            'keyword.operator', 'text', 'storage.type', 'text', 'paren.lparen',
          ],
          regex: `(${identifierRe})(\\.)(${identifierRe})(\\s*)(=)(\\s*)(function)(\\s*)(\\()`,
          next: 'function_arguments',
        }, {
          token: [
            'entity.name.function', 'text', 'keyword.operator', 'text', 'storage.type',
            'text', 'paren.lparen',
          ],
          regex: `(${identifierRe})(\\s*)(=)(\\s*)(function)(\\s*)(\\()`,
          next: 'function_arguments',
        }, {
          token: [
            'storage.type', 'punctuation.operator', 'entity.name.function', 'text',
            'keyword.operator', 'text',
            'storage.type', 'text', 'entity.name.function', 'text', 'paren.lparen',
          ],
          regex: `(${identifierRe})(\\.)(${identifierRe})(\\s*)(=)(\\s*)(function)(\\s+)(\\w+)(\\s*)(\\()`,
          next: 'function_arguments',
        }, {
          token: [
            'storage.type', 'text', 'entity.name.function', 'text', 'paren.lparen',
          ],
          regex: `(function)(\\s+)(${identifierRe})(\\s*)(\\()`,
          next: 'function_arguments',
        }, {
          token: [
            'entity.name.function', 'text', 'punctuation.operator',
            'text', 'storage.type', 'text', 'paren.lparen',
          ],
          regex: `(${identifierRe})(\\s*)(:)(\\s*)(function)(\\s*)(\\()`,
          next: 'function_arguments',
        }, {
          token: [
            'text', 'text', 'storage.type', 'text', 'paren.lparen',
          ],
          regex: '(:)(\\s*)(function)(\\s*)(\\()',
          next: 'function_arguments',
        }, {
          token: 'keyword',
          regex: "from(?=\\s*('|\"))",
        }, {
          token: 'keyword',
          regex: `(?:${kwBeforeRe})\\b`,
          next: 'start',
        }, {
          token: ['support.constant'],
          regex: /that\b/,
        }, {
          token: ['storage.type', 'punctuation.operator', 'support.function.firebug'],
          regex: /(console)(\.)(warn|info|log|error|time|trace|timeEnd|assert)\b/,
        }, {
          token: keywordMapper,
          regex: identifierRe,
        }, {
          token: 'punctuation.operator',
          regex: /[.](?![.])/,
          next: 'property',
        }, {
          token: 'storage.type',
          regex: /=>/,
          next: 'start',
        }, {
          token: 'keyword.operator',
          regex: /--|\+\+|\.{3}|===|==|=|!=|!==|<+=?|>+=?|!|&&|\|\||\?:|[!$%&*+\-~\/^]=?/,
          next: 'start',
        }, {
          token: 'punctuation.operator',
          regex: /[?:,;.]/,
          next: 'start',
        }, {
          token: 'paren.lparen',
          regex: /[\[({]/,
          next: 'start',
        }, {
          token: 'paren.rparen',
          regex: /[\])}]/,
        }, {
          token: 'comment',
          regex: /^#!.*$/,
        },
      ],
      property: [{
        token: 'text',
        regex: '\\s+',
      }, {
        token: [
          'storage.type', 'punctuation.operator', 'entity.name.function', 'text',
          'keyword.operator', 'text',
          'storage.type', 'text', 'entity.name.function', 'text', 'paren.lparen',
        ],
        regex: `(${identifierRe})(\\.)(${identifierRe})(\\s*)(=)(\\s*)(function)(?:(\\s+)(\\w+))?(\\s*)(\\()`,
        next: 'function_arguments',
      }, {
        token: 'punctuation.operator',
        regex: /[.](?![.])/,
      }, {
        token: 'support.function',
        regex: /(s(?:h(?:ift|ow(?:Mod(?:elessDialog|alDialog)|Help))|croll(?:X|By(?:Pages|Lines)?|Y|To)?|t(?:op|rike)|i(?:n|zeToContent|debar|gnText)|ort|u(?:p|b(?:str(?:ing)?)?)|pli(?:ce|t)|e(?:nd|t(?:Re(?:sizable|questHeader)|M(?:i(?:nutes|lliseconds)|onth)|Seconds|Ho(?:tKeys|urs)|Year|Cursor|Time(?:out)?|Interval|ZOptions|Date|UTC(?:M(?:i(?:nutes|lliseconds)|onth)|Seconds|Hours|Date|FullYear)|FullYear|Active)|arch)|qrt|lice|avePreferences|mall)|h(?:ome|andleEvent)|navigate|c(?:har(?:CodeAt|At)|o(?:s|n(?:cat|textual|firm)|mpile)|eil|lear(?:Timeout|Interval)?|a(?:ptureEvents|ll)|reate(?:StyleSheet|Popup|EventObject))|t(?:o(?:GMTString|S(?:tring|ource)|U(?:TCString|pperCase)|Lo(?:caleString|werCase))|est|a(?:n|int(?:Enabled)?))|i(?:s(?:NaN|Finite)|ndexOf|talics)|d(?:isableExternalCapture|ump|etachEvent)|u(?:n(?:shift|taint|escape|watch)|pdateCommands)|j(?:oin|avaEnabled)|p(?:o(?:p|w)|ush|lugins.refresh|a(?:ddings|rse(?:Int|Float)?)|r(?:int|ompt|eference))|e(?:scape|nableExternalCapture|val|lementFromPoint|x(?:p|ec(?:Script|Command)?))|valueOf|UTC|queryCommand(?:State|Indeterm|Enabled|Value)|f(?:i(?:nd|le(?:ModifiedDate|Size|CreatedDate|UpdatedDate)|xed)|o(?:nt(?:size|color)|rward)|loor|romCharCode)|watch|l(?:ink|o(?:ad|g)|astIndexOf)|a(?:sin|nchor|cos|t(?:tachEvent|ob|an(?:2)?)|pply|lert|b(?:s|ort))|r(?:ou(?:nd|teEvents)|e(?:size(?:By|To)|calc|turnValue|place|verse|l(?:oad|ease(?:Capture|Events)))|andom)|g(?:o|et(?:ResponseHeader|M(?:i(?:nutes|lliseconds)|onth)|Se(?:conds|lection)|Hours|Year|Time(?:zoneOffset)?|Da(?:y|te)|UTC(?:M(?:i(?:nutes|lliseconds)|onth)|Seconds|Hours|Da(?:y|te)|FullYear)|FullYear|A(?:ttention|llResponseHeaders)))|m(?:in|ove(?:B(?:y|elow)|To(?:Absolute)?|Above)|ergeAttributes|a(?:tch|rgins|x))|b(?:toa|ig|o(?:ld|rderWidths)|link|ack))\b(?=\()/,
      }, {
        token: 'support.function.dom',
        regex: /(s(?:ub(?:stringData|mit)|plitText|e(?:t(?:NamedItem|Attribute(?:Node)?)|lect))|has(?:ChildNodes|Feature)|namedItem|c(?:l(?:ick|o(?:se|neNode))|reate(?:C(?:omment|DATASection|aption)|T(?:Head|extNode|Foot)|DocumentFragment|ProcessingInstruction|E(?:ntityReference|lement)|Attribute))|tabIndex|i(?:nsert(?:Row|Before|Cell|Data)|tem)|open|delete(?:Row|C(?:ell|aption)|T(?:Head|Foot)|Data)|focus|write(?:ln)?|a(?:dd|ppend(?:Child|Data))|re(?:set|place(?:Child|Data)|move(?:NamedItem|Child|Attribute(?:Node)?)?)|get(?:NamedItem|Element(?:sBy(?:Name|TagName|ClassName)|ById)|Attribute(?:Node)?)|blur)\b(?=\()/,
      }, {
        token: 'support.constant',
        regex: /(s(?:ystemLanguage|cr(?:ipts|ollbars|een(?:X|Y|Top|Left))|t(?:yle(?:Sheets)?|atus(?:Text|bar)?)|ibling(?:Below|Above)|ource|uffixes|e(?:curity(?:Policy)?|l(?:ection|f)))|h(?:istory|ost(?:name)?|as(?:h|Focus))|y|X(?:MLDocument|SLDocument)|n(?:ext|ame(?:space(?:s|URI)|Prop))|M(?:IN_VALUE|AX_VALUE)|c(?:haracterSet|o(?:n(?:structor|trollers)|okieEnabled|lorDepth|mp(?:onents|lete))|urrent|puClass|l(?:i(?:p(?:boardData)?|entInformation)|osed|asses)|alle(?:e|r)|rypto)|t(?:o(?:olbar|p)|ext(?:Transform|Indent|Decoration|Align)|ags)|SQRT(?:1_2|2)|i(?:n(?:ner(?:Height|Width)|put)|ds|gnoreCase)|zIndex|o(?:scpu|n(?:readystatechange|Line)|uter(?:Height|Width)|p(?:sProfile|ener)|ffscreenBuffering)|NEGATIVE_INFINITY|d(?:i(?:splay|alog(?:Height|Top|Width|Left|Arguments)|rectories)|e(?:scription|fault(?:Status|Ch(?:ecked|arset)|View)))|u(?:ser(?:Profile|Language|Agent)|n(?:iqueID|defined)|pdateInterval)|_content|p(?:ixelDepth|ort|ersonalbar|kcs11|l(?:ugins|atform)|a(?:thname|dding(?:Right|Bottom|Top|Left)|rent(?:Window|Layer)?|ge(?:X(?:Offset)?|Y(?:Offset)?))|r(?:o(?:to(?:col|type)|duct(?:Sub)?|mpter)|e(?:vious|fix)))|e(?:n(?:coding|abledPlugin)|x(?:ternal|pando)|mbeds)|v(?:isibility|endor(?:Sub)?|Linkcolor)|URLUnencoded|P(?:I|OSITIVE_INFINITY)|f(?:ilename|o(?:nt(?:Size|Family|Weight)|rmName)|rame(?:s|Element)|gColor)|E|whiteSpace|l(?:i(?:stStyleType|n(?:eHeight|kColor))|o(?:ca(?:tion(?:bar)?|lName)|wsrc)|e(?:ngth|ft(?:Context)?)|a(?:st(?:M(?:odified|atch)|Index|Paren)|yer(?:s|X)|nguage))|a(?:pp(?:MinorVersion|Name|Co(?:deName|re)|Version)|vail(?:Height|Top|Width|Left)|ll|r(?:ity|guments)|Linkcolor|bove)|r(?:ight(?:Context)?|e(?:sponse(?:XML|Text)|adyState))|global|x|m(?:imeTypes|ultiline|enubar|argin(?:Right|Bottom|Top|Left))|L(?:N(?:10|2)|OG(?:10E|2E))|b(?:o(?:ttom|rder(?:Width|RightWidth|BottomWidth|Style|Color|TopWidth|LeftWidth))|ufferDepth|elow|ackground(?:Color|Image)))\b/,
      }, {
        token: 'identifier',
        regex: identifierRe,
      }, {
        regex: '',
        token: 'empty',
        next: 'no_regex',
      },
      ],
      start: [
        DocCommentHighlightRules.getStartRule('doc-start'),
        comments('start'),
        {
          token: 'string.regexp',
          regex: '\\/',
          next: 'regex',
        }, {
          token: 'text',
          regex: '\\s+|^$',
          next: 'start',
        }, {
          token: 'empty',
          regex: '',
          next: 'no_regex',
        },
      ],
      regex: [
        {
          token: 'regexp.keyword.operator',
          regex: '\\\\(?:u[\\da-fA-F]{4}|x[\\da-fA-F]{2}|.)',
        }, {
          token: 'string.regexp',
          regex: '/[sxngimy]*',
          next: 'no_regex',
        }, {
          token: 'invalid',
          regex: /\{\d+\b,?\d*\}[+*]|[+*$^?][+*]|[$^][?]|\?{3,}/,
        }, {
          token: 'constant.language.escape',
          regex: /\(\?[:=!]|\)|\{\d+\b,?\d*\}|[+*]\?|[()$^+*?.]/,
        }, {
          token: 'constant.language.delimiter',
          regex: /\|/,
        }, {
          token: 'constant.language.escape',
          regex: /\[\^?/,
          next: 'regex_character_class',
        }, {
          token: 'empty',
          regex: '$',
          next: 'no_regex',
        }, {
          defaultToken: 'string.regexp',
        },
      ],
      regex_character_class: [
        {
          token: 'regexp.charclass.keyword.operator',
          regex: '\\\\(?:u[\\da-fA-F]{4}|x[\\da-fA-F]{2}|.)',
        }, {
          token: 'constant.language.escape',
          regex: ']',
          next: 'regex',
        }, {
          token: 'constant.language.escape',
          regex: '-',
        }, {
          token: 'empty',
          regex: '$',
          next: 'no_regex',
        }, {
          defaultToken: 'string.regexp.charachterclass',
        },
      ],
      function_arguments: [
        {
          token: 'variable.parameter',
          regex: identifierRe,
        }, {
          token: 'punctuation.operator',
          regex: '[, ]+',
        }, {
          token: 'punctuation.operator',
          regex: '$',
        }, {
          token: 'empty',
          regex: '',
          next: 'no_regex',
        },
      ],
      qqstring: [
        {
          token: 'constant.language.escape',
          regex: escapedRe,
        }, {
          token: 'string',
          regex: '\\\\$',
          consumeLineEnd: true,
        }, {
          token: 'string',
          regex: '"|$',
          next: 'no_regex',
        }, {
          defaultToken: 'string',
        },
      ],
      qstring: [
        {
          token: 'constant.language.escape',
          regex: escapedRe,
        }, {
          token: 'string',
          regex: '\\\\$',
          consumeLineEnd: true,
        }, {
          token: 'string',
          regex: "'|$",
          next: 'no_regex',
        }, {
          defaultToken: 'string',
        },
      ],
    };


    if (!options || !options.noES6) {
      this.$rules.no_regex.unshift({
        regex: '[{}]',
        onMatch(val, state, stack) {
          this.next = val == '{' ? this.nextState : '';
          if (val == '{' && stack.length) {
            stack.unshift('start', state);
          } else if (val == '}' && stack.length) {
            stack.shift();
            this.next = stack.shift();
            if (this.next.indexOf('string') != -1 || this.next.indexOf('jsx') != -1) { return 'paren.quasi.end'; }
          }
          return val == '{' ? 'paren.lparen' : 'paren.rparen';
        },
        nextState: 'start',
      }, {
        token: 'string.quasi.start',
        regex: /`/,
        push: [{
          token: 'constant.language.escape',
          regex: escapedRe,
        }, {
          token: 'paren.quasi.start',
          regex: /\${/,
          push: 'start',
        }, {
          token: 'string.quasi.end',
          regex: /`/,
          next: 'pop',
        }, {
          defaultToken: 'string.quasi',
        }],
      });

      if (!options || options.jsx != false) { JSX.call(this); }
    }

    this.embedRules(DocCommentHighlightRules, 'doc-',
      [DocCommentHighlightRules.getEndRule('no_regex')]);

    this.normalizeRules();
  };

  oop.inherits(JavaScriptHighlightRules, TextHighlightRules);

  function JSX() {
    const tagRegex = identifierRe.replace('\\d', '\\d\\-');
    const jsxTag = {
      onMatch(val, state, stack) {
        const offset = val.charAt(1) == '/' ? 2 : 1;
        if (offset == 1) {
          if (state != this.nextState) { stack.unshift(this.next, this.nextState, 0); } else { stack.unshift(this.next); }
          stack[2]++;
        } else if (offset == 2) {
          if (state == this.nextState) {
            stack[1]--;
            if (!stack[1] || stack[1] < 0) {
              stack.shift();
              stack.shift();
            }
          }
        }
        return [{
          type: `meta.tag.punctuation.${offset == 1 ? '' : 'end-'}tag-open.xml`,
          value: val.slice(0, offset),
        }, {
          type: 'meta.tag.tag-name.xml',
          value: val.substr(offset),
        }];
      },
      regex: `</?${tagRegex}`,
      next: 'jsxAttributes',
      nextState: 'jsx',
    };
    this.$rules.start.unshift(jsxTag);
    const jsxJsRule = {
      regex: '{',
      token: 'paren.quasi.start',
      push: 'start',
    };
    this.$rules.jsx = [
      jsxJsRule,
      jsxTag,
      { include: 'reference' },
      { defaultToken: 'string' },
    ];
    this.$rules.jsxAttributes = [{
      token: 'meta.tag.punctuation.tag-close.xml',
      regex: '/?>',
      onMatch(value, currentState, stack) {
        if (currentState == stack[0]) { stack.shift(); }
        if (value.length == 2) {
          if (stack[0] == this.nextState) { stack[1]--; }
          if (!stack[1] || stack[1] < 0) {
            stack.splice(0, 2);
          }
        }
        this.next = stack[0] || 'start';
        return [{ type: this.token, value: value }];
      },
      nextState: 'jsx',
    },
    jsxJsRule,
    comments('jsxAttributes'),
    {
      token: 'entity.other.attribute-name.xml',
      regex: tagRegex,
    }, {
      token: 'keyword.operator.attribute-equals.xml',
      regex: '=',
    }, {
      token: 'text.tag-whitespace.xml',
      regex: '\\s+',
    }, {
      token: 'string.attribute-value.xml',
      regex: "'",
      stateName: 'jsx_attr_q',
      push: [
        { token: 'string.attribute-value.xml', regex: "'", next: 'pop' },
        { include: 'reference' },
        { defaultToken: 'string.attribute-value.xml' },
      ],
    }, {
      token: 'string.attribute-value.xml',
      regex: '"',
      stateName: 'jsx_attr_qq',
      push: [
        { token: 'string.attribute-value.xml', regex: '"', next: 'pop' },
        { include: 'reference' },
        { defaultToken: 'string.attribute-value.xml' },
      ],
    },
    jsxTag,
    ];
    this.$rules.reference = [{
      token: 'constant.language.escape.reference.xml',
      regex: '(?:&#[0-9]+;)|(?:&#x[0-9a-fA-F]+;)|(?:&[a-zA-Z0-9_:\\.-]+;)',
    }];
  }

  function comments(next) {
    return [
      {
        token: 'comment', // multi line comment
        regex: /\/\*/,
        next: [
          DocCommentHighlightRules.getTagRule(),
          { token: 'comment', regex: '\\*\\/', next: next || 'pop' },
          { defaultToken: 'comment', caseInsensitive: true },
        ],
      }, {
        token: 'comment',
        regex: '\\/\\/',
        next: [
          DocCommentHighlightRules.getTagRule(),
          { token: 'comment', regex: '$|^', next: next || 'pop' },
          { defaultToken: 'comment', caseInsensitive: true },
        ],
      },
    ];
  }
  exports.JavaScriptHighlightRules = JavaScriptHighlightRules;
});

ace.define('ace/mode/matching_brace_outdent', ['require', 'exports', 'module', 'ace/range'], (require, exports, module) => {
  const { Range } = require('../range');

  const MatchingBraceOutdent = function () {};

  (function () {
    this.checkOutdent = function (line, input) {
      if (!/^\s+$/.test(line)) { return false; }

      return /^\s*\}/.test(input);
    };

    this.autoOutdent = function (doc, row) {
      const line = doc.getLine(row);
      const match = line.match(/^(\s*\})/);

      if (!match) return 0;

      const column = match[1].length;
      const openBracePos = doc.findMatchingBracket({ row: row, column: column });

      if (!openBracePos || openBracePos.row == row) return 0;

      const indent = this.$getIndent(doc.getLine(openBracePos.row));
      doc.replace(new Range(row, 0, row, column - 1), indent);
    };

    this.$getIndent = function (line) {
      return line.match(/^\s*/)[0];
    };
  }).call(MatchingBraceOutdent.prototype);

  exports.MatchingBraceOutdent = MatchingBraceOutdent;
});

ace.define('ace/mode/folding/cstyle', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/range', 'ace/mode/folding/fold_mode'], (require, exports, module) => {
  const oop = require('../../lib/oop');
  const { Range } = require('../../range');
  const BaseFoldMode = require('./fold_mode').FoldMode;

  const FoldMode = exports.FoldMode = function (commentRegex) {
    if (commentRegex) {
      this.foldingStartMarker = new RegExp(
        this.foldingStartMarker.source.replace(/\|[^|]*?$/, `|${commentRegex.start}`),
      );
      this.foldingStopMarker = new RegExp(
        this.foldingStopMarker.source.replace(/\|[^|]*?$/, `|${commentRegex.end}`),
      );
    }
  };
  oop.inherits(FoldMode, BaseFoldMode);

  (function () {
    this.foldingStartMarker = /([\{\[\(])[^\}\]\)]*$|^\s*(\/\*)/;
    this.foldingStopMarker = /^[^\[\{\(]*([\}\]\)])|^[\s\*]*(\*\/)/;
    this.singleLineBlockCommentRe = /^\s*(\/\*).*\*\/\s*$/;
    this.tripleStarBlockCommentRe = /^\s*(\/\*\*\*).*\*\/\s*$/;
    this.startRegionRe = /^\s*(\/\*|\/\/)#?region\b/;
    this._getFoldWidgetBase = this.getFoldWidget;
    this.getFoldWidget = function (session, foldStyle, row) {
      const line = session.getLine(row);

      if (this.singleLineBlockCommentRe.test(line)) {
        if (!this.startRegionRe.test(line) && !this.tripleStarBlockCommentRe.test(line)) { return ''; }
      }

      const fw = this._getFoldWidgetBase(session, foldStyle, row);

      if (!fw && this.startRegionRe.test(line)) { return 'start'; } // lineCommentRegionStart

      return fw;
    };

    this.getFoldWidgetRange = function (session, foldStyle, row, forceMultiline) {
      const line = session.getLine(row);

      if (this.startRegionRe.test(line)) { return this.getCommentRegionBlock(session, line, row); }

      var match = line.match(this.foldingStartMarker);
      if (match) {
        var i = match.index;

        if (match[1]) { return this.openingBracketBlock(session, match[1], row, i); }

        let range = session.getCommentFoldRange(row, i + match[0].length, 1);

        if (range && !range.isMultiLine()) {
          if (forceMultiline) {
            range = this.getSectionRange(session, row);
          } else if (foldStyle != 'all') { range = null; }
        }

        return range;
      }

      if (foldStyle === 'markbegin') { return; }

      var match = line.match(this.foldingStopMarker);
      if (match) {
        var i = match.index + match[0].length;

        if (match[1]) { return this.closingBracketBlock(session, match[1], row, i); }

        return session.getCommentFoldRange(row, i, -1);
      }
    };

    this.getSectionRange = function (session, row) {
      let line = session.getLine(row);
      const startIndent = line.search(/\S/);
      const startRow = row;
      const startColumn = line.length;
      row += 1;
      let endRow = row;
      const maxRow = session.getLength();
      while (++row < maxRow) {
        line = session.getLine(row);
        const indent = line.search(/\S/);
        if (indent === -1) { continue; }
        if (startIndent > indent) { break; }
        const subRange = this.getFoldWidgetRange(session, 'all', row);

        if (subRange) {
          if (subRange.start.row <= startRow) {
            break;
          } else if (subRange.isMultiLine()) {
            row = subRange.end.row;
          } else if (startIndent == indent) {
            break;
          }
        }
        endRow = row;
      }

      return new Range(startRow, startColumn, endRow, session.getLine(endRow).length);
    };
    this.getCommentRegionBlock = function (session, line, row) {
      const startColumn = line.search(/\s*$/);
      const maxRow = session.getLength();
      const startRow = row;

      const re = /^\s*(?:\/\*|\/\/|--)#?(end)?region\b/;
      let depth = 1;
      while (++row < maxRow) {
        line = session.getLine(row);
        const m = re.exec(line);
        if (!m) continue;
        if (m[1]) depth--;
        else depth++;

        if (!depth) break;
      }

      const endRow = row;
      if (endRow > startRow) {
        return new Range(startRow, startColumn, endRow, line.length);
      }
    };
  }).call(FoldMode.prototype);
});

ace.define('ace/mode/javascript', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/text', 'ace/mode/javascript_highlight_rules', 'ace/mode/matching_brace_outdent', 'ace/worker/worker_client', 'ace/mode/behaviour/cstyle', 'ace/mode/folding/cstyle'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const TextMode = require('./text').Mode;
  const { JavaScriptHighlightRules } = require('./javascript_highlight_rules');
  const { MatchingBraceOutdent } = require('./matching_brace_outdent');
  const { WorkerClient } = require('../worker/worker_client');
  const { CstyleBehaviour } = require('./behaviour/cstyle');
  const CStyleFoldMode = require('./folding/cstyle').FoldMode;

  const Mode = function () {
    this.HighlightRules = JavaScriptHighlightRules;

    this.$outdent = new MatchingBraceOutdent();
    this.$behaviour = new CstyleBehaviour();
    this.foldingRules = new CStyleFoldMode();
  };
  oop.inherits(Mode, TextMode);

  (function () {
    this.lineCommentStart = '//';
    this.blockComment = { start: '/*', end: '*/' };
    this.$quotes = { '"': '"', "'": "'", '`': '`' };

    this.getNextLineIndent = function (state, line, tab) {
      let indent = this.$getIndent(line);

      const tokenizedLine = this.getTokenizer().getLineTokens(line, state);
      const { tokens } = tokenizedLine;
      const endState = tokenizedLine.state;

      if (tokens.length && tokens[tokens.length - 1].type == 'comment') {
        return indent;
      }

      if (state == 'start' || state == 'no_regex') {
        var match = line.match(/^.*(?:\bcase\b.*:|[\{\(\[])\s*$/);
        if (match) {
          indent += tab;
        }
      } else if (state == 'doc-start') {
        if (endState == 'start' || endState == 'no_regex') {
          return '';
        }
        var match = line.match(/^\s*(\/?)\*/);
        if (match) {
          if (match[1]) {
            indent += ' ';
          }
          indent += '* ';
        }
      }

      return indent;
    };

    this.checkOutdent = function (state, line, input) {
      return this.$outdent.checkOutdent(line, input);
    };

    this.autoOutdent = function (state, doc, row) {
      this.$outdent.autoOutdent(doc, row);
    };

    this.createWorker = function (session) {
      const worker = new WorkerClient(['ace'], 'ace/mode/javascript_worker', 'JavaScriptWorker');
      worker.attachToDocument(session.getDocument());

      worker.on('annotate', (results) => {
        session.setAnnotations(results.data);
      });

      worker.on('terminate', () => {
        session.clearAnnotations();
      });

      return worker;
    };

    this.$id = 'ace/mode/javascript';
  }).call(Mode.prototype);

  exports.Mode = Mode;
});

ace.define('ace/mode/graylog_highlight_rules', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/doc_comment_highlight_rules', 'ace/mode/text_highlight_rules'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const { DocCommentHighlightRules } = require('./doc_comment_highlight_rules');
  const { TextHighlightRules } = require('./text_highlight_rules');

  const GraylogHighlightRules = function () {
    const keywords = 'let|rule|match|pipeline';

    const builtinConstants = 'all|either|and|or|not|during|when|then|end';

    const builtinFunctions = 'to_bool|to_double|to_long|to_string|to_map|is_bool|is_number|is_double|is_long|is_string|'
        + 'is_collection|is_list|is_map|is_date|is_period|is_ip|is_json|is_url|has_field|set_field|'
        + 'set_fields|rename_field|remove_field|drop_message|create_message|clone_message|'
        + 'remove_from_stream|route_to_stream|from_input|regex|regex_replace|grok|grok_exists|abbreviate|'
        + 'capitalize|contains|ends_with|lowercase|substring|swapcase|uncapitalize|uppercase|concat|key_value|'
        + 'join|split|starts_with|replace|parse_json|select_jsonpath|to_date|now|parse_date|'
        + 'parse_unix_milliseconds|flex_parse_date|format_date|years|months|weeks|days|hours|minutes|seconds|'
        + 'millis|period|crc32|crc32c|md5|murmur3_32|murmur3_128|sha1|sha256|sha512|base16_encode|base16_decode|'
        + 'base32_encode|base32_decode|base32human_encode|base32human_decode|base64_encode|base64_decode|'
        + 'base64url_encode|base64url_decode|cidr_match|to_ip|is_null|is_not_null|to_url|urldecode|urlencode|'
        + 'syslog_facility|syslog_level|expand_syslog_priority|expand_syslog_priority_as_string|lookup|'
        + 'lookup_value|debug|parse_cef|otx_lookup_domain|otx_lookup_ip|tor_lookup|spamhaus_lookup_ip|'
        + 'abusech_ransom_lookup_domain|abusech_ransom_lookup_ip|threat_intel_lookup_ip|'
        + 'threat_intel_lookup_domain|whois_lookup_ip|in_private_net';

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


    this.embedRules(DocCommentHighlightRules, 'doc-',
      [DocCommentHighlightRules.getEndRule('start')]);
    this.normalizeRules();
  };

  oop.inherits(GraylogHighlightRules, TextHighlightRules);

  exports.GraylogHighlightRules = GraylogHighlightRules;
});

ace.define('ace/mode/folding/graylog', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/folding/cstyle', 'ace/range'], (require, exports, module) => {
  const oop = require('../../lib/oop');
  const CStyleFoldMode = require('./cstyle').FoldMode;
  const { Range } = require('../../range');

  const FoldMode = exports.FoldMode = function () {};
  oop.inherits(FoldMode, CStyleFoldMode);

  (function () {
    this.importRegex = /^import /;
    this.getCStyleFoldWidget = this.getFoldWidget;
    this.getFoldWidget = function (session, foldStyle, row) {
      if (foldStyle === 'markbegin') {
        const line = session.getLine(row);
        if (this.importRegex.test(line)) {
          if (row == 0 || !this.importRegex.test(session.getLine(row - 1))) { return 'start'; }
        }
      }

      return this.getCStyleFoldWidget(session, foldStyle, row);
    };

    this.getCstyleFoldWidgetRange = this.getFoldWidgetRange;
    this.getFoldWidgetRange = function (session, foldStyle, row, forceMultiline) {
      var line = session.getLine(row);
      const match = line.match(this.importRegex);
      if (!match || foldStyle !== 'markbegin') { return this.getCstyleFoldWidgetRange(session, foldStyle, row, forceMultiline); }

      const startColumn = match[0].length;
      const maxRow = session.getLength();
      const startRow = row;
      let endRow = row;

      while (++row < maxRow) {
        var line = session.getLine(row);
        if (line.match(/^\s*$/)) { continue; }

        if (!line.match(this.importRegex)) { break; }

        endRow = row;
      }

      if (endRow > startRow) {
        const endColumn = session.getLine(endRow).length;
        return new Range(startRow, startColumn, endRow, endColumn);
      }
    };
  }).call(FoldMode.prototype);
});

ace.define('ace/mode/graylog', ['require', 'exports', 'module', 'ace/lib/oop', 'ace/mode/javascript', 'ace/mode/graylog_highlight_rules', 'ace/mode/folding/graylog'], (require, exports, module) => {
  const oop = require('../lib/oop');
  const JavaScriptMode = require('./javascript').Mode;
  const { GraylogHighlightRules } = require('./graylog_highlight_rules');
  const GraylogFoldMode = require('./folding/graylog').FoldMode;

  const Mode = function () {
    JavaScriptMode.call(this);
    this.HighlightRules = GraylogHighlightRules;
    this.foldingRules = new GraylogFoldMode();
  };
  oop.inherits(Mode, JavaScriptMode);

  (function () {
    this.createWorker = function (session) {
      return null;
    };

    this.$id = 'ace/mode/graylog';
  }).call(Mode.prototype);

  exports.Mode = Mode;
}); (function () {
  ace.require(['ace/mode/graylog'], (m) => {
    if (typeof module === 'object' && typeof exports === 'object' && module) {
      module.exports = m;
    }
  });
}());
