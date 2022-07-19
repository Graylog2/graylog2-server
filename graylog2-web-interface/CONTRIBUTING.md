# Contributing

Please follow [the instructions on graylog.org](https://www.graylog.org/get-involved/).

## Code of Conduct
In the interest of fostering an open and welcoming environment, we as
contributors and maintainers pledge to making participation in our project and
our community a harassment-free experience for everyone, regardless of age, body
size, disability, ethnicity, gender identity and expression, level of experience,
nationality, personal appearance, race, religion, or sexual identity and
orientation.

Please read and understand the [Code of Conduct](https://github.com/Graylog2/graylog2-server/blob/master/CODE_OF_CONDUCT.md).

## Conventions

### Consistent Code Style
- We use ESLint to detect some issues in our code. We mostly follow the [Airbnb Javascript style guide](https://github.com/airbnb/javascript) to write frontend code, with a few exceptions.
- We maintain those custom rules in a package, which is part of [our graylog2-server repository](https://raw.githubusercontent.com/Graylog2/graylog2-server/master/graylog2-web-interface/packages/eslint-config-graylog/index.js).
- Ensure you are seeing linter hints in you IDE and consider enabling the setting "fix linter hints on save" ([IntelliJ Docs](https://www.jetbrains.com/help/idea/eslint.html#ws_eslint_configure_run_eslint_on_save)).
- There is a CI job, which checks if there are linter hints in changes files.
- If you want to see all linter hints for changed files locally, you can run `yarn lint:changes`. You will need to commit your changes first.
- If you want to see linter hints for a specific file, you can run `yarn lint:path /path/to/file`.

### Naming
#### Function Naming
- Generally use a verb as a function name.

#### Class Naming
- Generally use a noun as a class name

### Class vs functional components
Small components should be functional components. When a component is more complex, you can decide which type of component
you want to use (class or a functional component with react hooks). When you don’t have a preference, use a functional component.

### Type definitions
- We use TypeScript for new components, and we also define `PropType` definitions. Static types add better type support for the development, integrating with your IDE, while `PropType` definitions are present at runtime.
- Names of unused parameters
  - Sometimes you need to define a parameter which is not being used, for example when you just want to use the second parameter. To satisfy TypeScript the parameter name needs to be prefixed with underscore.
  - Instead of defining just `_`, use a more meaningful name, like `_theParameterName`. (Related to [this discussion](https://github.com/Graylog2/graylog2-server/pull/12176#pullrequestreview-940555887)).
- `types.d.ts` can have hidden errors, like missing imports. Rename it temporary to `types.ts` to detect the most obvious ones.

### Imports
#### ES6 modules_
- Prefer ES6 modules (import and export) over a non-standard module system and CommonJS’s require.

#### Modules
- Modules with one export should use default export.
- When a module has several exports, you should use the default export only if one of the exports serves the main purpose of the module.
- Sometimes, specially when working on a folder containing common components, it may be useful to add an index.js file with all exports of that folder, allowing consumers of those exports to combine several imports in the same line.
- The downside of this is that it might introduce cyclic dependencies (which can be resolved by babel/webpack by proxying, but should be avoided)

### React components
- Keep components small, as in < 300 lines
- Keep components simple, so they do not contain real logic. You need to transform the structure of an object when a user clicks on a button? Create a helper function for the logic which can be reused. Place the helper function in a place where it can be found easily.

### Testing
- When adding new functionality, try to write unit tests for every possible use case. If you are not sure where to start, try to test what is important for the user.
- Currently, we have nearly only unit tests, but want to add more integration tests in the future. You can find examples for an integration test in `src/views/spec` 
- There are still some tests which use `enzyme`, but we are only using `@testing-library/react` for new tests. 
  We do favor `testing-library` for new tests, as it has been proven that it results in more reliable tests. You should not use both libraries in one file. In many cases, test quality can be improved by migrating a test suite from `enzyme` to `testing-library`.
- A test should not rely on the knowledge of the inner workings of the system under test. Especially when testing React components, we prefer to test it like a user would use it.
- Test files should be on the same level 
  - `ComponentA.jsx` 
  - `ComponentA.test.jsx` 
- Except you have to create a few fixtures, in this case create a __tests__ directory. 
  - ComponentA.jsx
  - `__tests__/ComponentA.test.jsx`
  - `__tests__/ComponentA.test.case1.json`
  - `__tests__/ComponentA.text.case1.result.json`
- Do not use snapshot tests to test the state of a component.
  - Imagine the following case: User clicks on a button and we display an alert. You can test this behavior with a snapshot, but it has many disadvantages. You can also just use the react-testing-library method `getByText()` to check if the alert is being displayed
  - When you create a snapshot you will need to update the snapshot often, for example when the button style changes.
  - Good use case for snapshot tests: check if a complex result of a function call is correct.
- Test timeouts
  - We have some flaky tests which fail, when jest has less resources to run the tests and the execution of a test just takes more time. In this case you will see failing tests with the error message “Exceeded timeout of 5000 ms for a test”.
  - You can avoid this by increasing the timeout for jest tests. `yarn test --maxWorkers=150%`.
  - You can reduce also the timeout: `yarn test --maxWorkers=25%`. This can be helpful if you want to get an idea how the result looks on computers with less power.
- Chrome extension “Testing Playground”
  - Helps you find the best queries to select elements

### Injecting stores
When writing new code, you should prefer injecting stores into your components with CombinedProvider instead of using StoreProvider
and ActionsProvider. The *Providers use a registration system using a key in the window object to ensure that stores are loaded and initialized only once (making them effectively singletons) across core & plugins.
Newer code (views) is testing a different approach: Instead of importing stores differently, it uses a `singleton()` wrapper that exports stores while tracking them centrally. The benefit is that:
- Tracking usages of stores across the code base & refactoring support of your favorite IDE still works
- (Re-)Moving a store that is still in use will fail at compile time, not at runtime
- Type information of the stores/actions is kept

### Good to know / gotchas

#### Default Values
- Using if and an assignment or the logical or assignment, you may end up assigning a default value when you didn't want to do it, e.g.
```js
const a = undefined || 'default';
const b = null || 'default';
const c = false || 'default';
const d = 0 || 'default';
const e = '' || 'default';
// They will all contain the 'default' value.
```
- Using default values on functions and destructuring only assigns default values when initial value was `undefined`, e.g.
```js
const testDefaultValues = ({ value1 = 12, value2 = 34 }) => {
  console.log(value1, value2);
}
testDefaultValues({ value1: undefined, value2: null })
// Output: 12, null
```
- With the introduction of support for nullish coalescing, we should use ?? for default values:
```js
// These will contain 'default
const a = undefined ?? 'default'
const b = null ?? 'default'
// These will contain the original value
const c = false ?? 'default'
const d = 0 ?? 'default'
const e = '' ?? 'default'
```

#### Avoid `Array.reduce` for object construction
Using `Array.reduce` to construct an object from an array is a common pattern. When working with many items (like hundreds or thousands) it can become really slow. 
Using `Object.fromEntries` is a better alternative in most cases. Have a look at [this PR](https://github.com/Graylog2/graylog2-server/pull/12162) for more information.

### Reusing components
- We use react-bootstrap for many UI common components. To help us deal with breaking changes in their APIs, and to style the components as we want, we use our own wrappers around react bootstrap components. You can import these components from components/graylog.
- Please ensure to check the [frontend documentation](https://graylog2.github.io/frontend-documentation) to see which common components we use. Whenever possible try using a common component, since that help us have a more consistent UI and make components used in different parts of the product behave the same way.

### Internal packages
We currently have a few internal packages, used for the core application and all plug-ins:
- `graylog-web-plugin` contains common packages for both the core and plug-ins, webpack configuration for building plug-ins, as well as some interfaces to register and consume plugins.
- `eslint-config-graylog` contains our custom linter configuration, based on eslint-config-airbnb.
- `stylelint-config-graylog` contains our custom stylelint configuration, based on the default stylelint config.

### Browser compatibility
We currently do not have a pre-release process to test different or old browser. Nevertheless you should test layout changes in at least all modern browsers (Chrome, Firefox, Safari). Bigger layout changes should also be tested in older browsers. Have a look at our [public browser compatibility list](https://docs.graylog.org/docs/web-interface#browser-compatibility).

### Plugin System
- Graylog's plugin system allows users to extend the graylog core product without changing its source code. There are multiple points in the application which can be extended, like the navigation.
- In the frontend you can register anything to the plugin store with `PluginStore.register(new PluginManifest({}, { thePluginStoreKey: [thePluginData] }));` in a plugin and use it with `usePluginEntities('thePluginStoreKey');`.
- Currently, there is no documentation about the available plugin store keys, you need to look up the usage in the code. 
- If you want to test the UI without plugins you can run `disable_plugins=true yarn start`.
- If you want to create a new plugin please have a look at [this example](https://github.com/Graylog2/graylog-plugin-sample) for more information. 

### Refactoring existing code
- When you are working on a feature, you will sometimes need to do some small changes on related files.
- Fix visible eslint warning in these files (such as deconstructing the props)
  - There is a CI job (ci-web-linter) that turns green when all linter hints are fixed
  - If fixing the linter hints is too much for the scope of the PR (e.g. a bugfix PR for a patch release), fixing them can be omitted and the PR merged even if that job is red. This should be an exemption though.
- Separate the refactoring/fixing of linter hints in (a) separate commit(s)
- When the refactoring gets too big, create a separate PR.
- When making changes closer to a release or working on changes that will be backported, please consider the risk of doing the refactoring of fixing linter errors now. If it feels too risky or there are too many changes, it may be a sign that the refactoring should wait.

### Working on a feature
- Especially for more complex changes it is helpful to test different scenarios
  - Test with different roles. For example not only with an admin user, but also with a user who has only the reader role and as little permissions as possible.
  - Test with different resolutions.
  - Test with different browsers (especially Safari).
  - Most important, test with homogeneous data and with a high volume of data.
- Execute checks locally before creating a PR
  - Running all checks in CI can take an hour, make sure you execute the most important ones locally first.
  - Run `yarn tsc && yarn lint:changes && yarn test` in at least the core and enterprise project.

### Common problems
- yarn cache
  - the yarn cache can get very big, like 200GB. Make sure to run `yarn cache clean` from time to time.

### UI Styling

#### Forms
- Latest research suggest forms with aligned labels and inputs are the easiest to use and navigate for users, so avoid using horizontal forms unless there is a good reason for it.
- Add helper text to help users fill the input.
- Only show validation state when something changed or the form was submitted.
- Prefer our own Select component over the default one.
- Avoid adding long forms into a modal.
- Mark optional fields as optional by adding “(Optional)” to the label text.
- We are working on using standard components and libraries to make our forms more consistent.

#### Responsive styles
- Using Graylog in large and medium size screens should be possible for all features in our product.
- Using Graylog in a mobile device may suppose some challenges, specially when graphs are an important part of the page.

#### Button sizes and colours
- Use grey (`default`) or blue (`info`) buttons to display neutral actions.
- Use red (`danger`) buttons to display actions that will destroy information.
- Use yellow (`warning`) buttons to display actions that may be dangerous.
- Use green (`success`) buttons to display actions that will create information.
- Avoid using `link` buttons, since that is considered misleading. Buttons and anchors have different purposes and usability, so we should try to use anchors only to navigate.

#### Page loading indicator
- Use a `Spinner` whenever a page is loading data from the backend.
- We are working on different levels of loading indicators.

### Modals
- Users should be able to close a modal with the keyboard, i.e. typing ESC.
- Modal forms should not close when clicking outside of the modal, since it is easy to lose some data.
- Modal forms should focus on the first input (you can use autofocus for that).
