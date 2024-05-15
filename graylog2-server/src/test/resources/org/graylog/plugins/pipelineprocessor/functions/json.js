"use strict";

const when = (msg) => true;
const then = (msg) => {
  let json = JSON.parse(msg.flat_json);
  functions.set_fields(json, null, null, null, null);

    // Don't fail on invalid input
    try {
      let invalid_json = JSON.parse('#FOOBAR#');
      functions.set_fields(invalid_json, null, null, null, null);
    } catch {}

    // Don't fail on empty input
    try {
      let empty_json = JSON.parse("");
      functions.set_fields(empty_json, null, null, null, null);
    } catch {}

    // Don't fail on nested input
    let nested_json = JSON.parse(msg.nested_json);
    functions.set_fields(nested_json, null, null, null, null);
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
