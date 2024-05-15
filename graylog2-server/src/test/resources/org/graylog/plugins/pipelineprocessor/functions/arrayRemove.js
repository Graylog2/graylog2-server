"use strict";

const when = (msg) => true;

const then = (msg) => {
  functions.set_field('remove_number', [1, 2, 3].filter(v => v !== 2), null, null, null, null, null);
  functions.set_field('remove_string', ["one", "two", "three"].filter(v => v !== "two"), null, null, null, null, null);
  functions.set_field('remove_missing', [1, 2, 3].filter(v => v !== 4), null, null, null, null, null);

  let array = [1, 2, 2];
  array.splice(array.indexOf(2), 1);
  functions.set_field('remove_only_one', array, null, null, null, null, null);

  functions.set_field('remove_all', [1, 2, 2].filter(v => v !== 2), null, null, null, null, null);

  functions.trigger_test();
};

export default { 'name': 'array_remove', 'when': when, 'then': then };

// rule "array_remove"
// when
//     true
// then
//     set_field("remove_number", array_remove([1, 2, 3], 2));
//     set_field("remove_string", array_remove(["one", "two", "three"], "two"));
//     set_field("remove_missing", array_remove([1, 2, 3], 4));
//     set_field("remove_only_one", array_remove([1, 2, 2], 2));
//     set_field("remove_all", array_remove([1, 2, 2], 2, true));
//     trigger_test();
// end
