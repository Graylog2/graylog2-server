Please help us to keep this documentation updated!

We all benefit from documenting our processes and components, as it help us to
use them without expending too much time reading at the source code, and also
to think twice about how we solved a certain problem.


## How to document your components

This guide was created using [React Styleguidist](https://react-styleguidist.js.org),
so that is a good place to get started if you want to know how to contribute to
this document.

Here is a summary of what to do if you just want to document a component:

1. Use [JSDoc](http://usejsdoc.org) to write documentation for the component
   and its props, as those are the main API the component provides to its
   consumers.
2. The component documentation **must** be placed just before the component
   declaration and **must** contain a brief explanation of what the component
   intents to do, followed by any warnings, notes, or deprecations if needed.
   Please **do not** include code examples in here unless you cannot create an
   example for some reason.
3. Each `propType` definition **must** be preceded by its documentation. Pay
   special attention to:

    - Document data structures the component expect in array and object props
    - Document when a callback function will be called. This is specially
      helpful if the prop name is not as clear as it should be
    - Document arguments any callback functions will receive

4. In order to write a usage example of the component, you need to create a
   [Markdown](https://en.wikipedia.org/wiki/Markdown) file in the same directory
   as the component, with the same name as the component but the `md` extension.
   E.g. `ReactGridContainer.md` will contain the examples for
   `ReactGridContainer.jsx`.
5. Only write examples to show important use cases of the component, but you
   do not need to write an example for each prop and value that the component
   can accept.
6. You can write React components in examples, and you should do it if you
   feel that showing how the state flows may help understanding how the
   component works.
7. Examples do not support `import` statements, but you can `require` any
   modules you need. Try to not require too many modules, though, as they will
   make the example more obscure and harder to understand. You can also use
   object destructuring when requiring more than one thing from the same module.
   E.g. `const { ButtonToolbar } = require('components/graylog');` can
   save a line of the example without making it harder to read.
