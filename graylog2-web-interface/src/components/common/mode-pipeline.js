import 'brace/mode/java';

class PipelineHighlightRules extends window.ace.acequire('ace/mode/text_highlight_rules').TextHighlightRules {
  constructor() {
    super();

    const keywords = 'let|rule|match|pipeline';

    const operators = 'and|or|not';

    const builtinConstants = 'all|either|during|when|then|end';

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
    this.HighlightRules = PipelineHighlightRules;
  }
}
