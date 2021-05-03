const config = {
  preset: 'jest-preset-graylog',
  setupFiles: [
    '<rootDir>/test/setup-jest.js',
  ],
  setupFilesAfterEnv: [
    'jest-enzyme',
    '<rootDir>/test/configure-testing-library.js',
  ],
  testEnvironment: '<rootDir>/test/integration-environment.js',
  testRegex: '\\.it\\.[jt]sx?$',
};

module.exports = config;
