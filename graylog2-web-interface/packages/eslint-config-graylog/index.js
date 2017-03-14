module.exports = {
  extends: [
    'eslint:recommended',
    'airbnb',
    'plugin:import/errors',
    'plugin:import/warnings',
    'plugin:import/react',
  ],
  plugins: [
    'import',
  ],
  rules: {
    'arrow-body-style': 0,
    'import/extensions': 0,
    'import/no-extraneous-dependencies': 0,
    'import/no-unresolved': 0,
    indent: [2, 2, { SwitchCase: 1 }],
    'max-len': 0,
    'new-cap': 0,
    'no-else-return': 1,
    'no-nested-ternary': 1,
    'no-underscore-dangle': 0,
    'object-shorthand': [2, 'methods'],
    'react/forbid-prop-types': 0,
    'react/jsx-closing-bracket-location': 0,
    'react/jsx-first-prop-new-line': 0,
    'react/jsx-indent-props': 0,
    'react/jsx-space-before-closing': 0,
    'react/prefer-es6-class': 0,
    'react/prefer-stateless-function': 1,
  }
};
