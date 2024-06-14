'use strict';

const when = (msg) => true;

const then = (msg, {set_field}) => {
  set_field('f1', 'v1', null, null, null, null, null);
  set_field('f_2', 'v_2', null, null, null, null, null);
  set_field('f 3', 'will be skipped', null, null, null, null, null);
  set_field('f 4', 'will be added with clean field param', null, null, null, null, true);
};

export default { 'name': 'grok', 'when': when, 'then': then };

// rule "set_field"
// when true
// then
// set_field(field: "f1", value: "v1");
// set_field(field: "f_2", value: "v_2");
// set_field(field: "f 3", value: "will be skipped");
// set_field(field: "f 4", value: "will be added with clean field param", clean_field: true);
// end
