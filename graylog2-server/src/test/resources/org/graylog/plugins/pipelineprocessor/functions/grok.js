'use strict';

const when = (msg) => true;

const then = (msg, {grok, set_fields}) => {
  let matches = grok('%{GREEDY:timestamp;date;yyyy-MM-dd\'T\'HH:mm:ss.SSSX}', '2015-07-31T10:05:36.773Z', false);
  set_fields(matches, null, null, null, null);

  // only named captures
  let matches1 = grok('%{NUM:num}', '10', true);
  set_fields(matches1, null, null, null, null);

  //test for underscore
  let matches2 = grok('%{UNDERSCORE}', 'test', true);
  set_fields(matches2, null, null, null, null);
};

export default { 'name': 'grok', 'when': when, 'then': then };

// rule "grok"
// when true
// then
//     let matches = grok(pattern: "%{GREEDY:timestamp;date;yyyy-MM-dd'T'HH:mm:ss.SSSX}", value: "2015-07-31T10:05:36.773Z");
//     set_fields(matches);
//
//     // only named captures
//     let matches1 = grok("%{NUM:num}", "10", true);
//     set_fields(matches1);
//
//     //test for underscore
//     let matches2 = grok("%{UNDERSCORE}", "test", true);
//     set_fields(matches2);
// end
