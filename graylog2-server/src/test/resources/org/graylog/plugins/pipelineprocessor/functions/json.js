'use strict';

const when = (msg) => true;
const then = (msg) => {

  Object.assign(msg, JSON.parse(msg.flat_json));

  // Don't fail on invalid input
  try {
    JSON.parse('#FOOBAR#');
    JSON.parse('');
  } catch {
  }

  // Don't fail on nested input
  Object.assign(msg, JSON.parse(msg.nested_json));
};

export default { 'name': 'json', 'when': when, 'then': then };

// rule "json"
// when
//   true
// then
//   let json = parse_json(to_string($message.flat_json));
//   set_fields(to_map(json));
//
//   // Don't fail on invalid input
//   let invalid_json = parse_json("#FOOBAR#");
//   set_fields(to_map(invalid_json));
//
//   // Don't fail on empty input
//   let empty_json = parse_json("");
//   set_fields(to_map(empty_json));
//
//   // Don't fail on nested input
//   let nested_json = parse_json(to_string($message.nested_json));
//   set_fields(to_map(nested_json));
// end
