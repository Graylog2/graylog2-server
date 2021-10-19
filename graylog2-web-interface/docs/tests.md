We use [Jest](https://facebook.github.io/jest/) and [React Testing-Library](https://testing-library.com/docs/react-testing-library/intro/)
to write frontend tests. We encourage you to write tests for any complex logic
that may be easily broken, but you may also write tests for components if you
really feel like it will help to make it more robust without much overhead.

New tests should import the Testing-Library's `render` function from `wrappedTestingLibrary`

**Example:** `import { render } from 'wrappedTestingLibrary';`
