module.exports = {
  rootDir: '../../',
  collectCoverageFrom: [
    'src/**/*.{js,jsx}',
  ],
  setupFiles: [
    'jest-localstorage-mock',
    require.resolve('./mock-FetchProvider.js'),
    require.resolve('./mock-Version.js'),
    require.resolve('./console-warnings-fail-tests.js'),
  ],
  setupFilesAfterEnv: [
    'jest-enzyme',
  ],
  moduleDirectories: [
    'src',
    'test',
    'node_modules',
  ],
  moduleFileExtensions: [
    'ts',
    'tsx',
    'js',
    'jsx',
  ],
  moduleNameMapper: {
    '^file-loader(\\?esModule=false)?!(.+)$': '$2',
    '^!style/useable!.*\\.(css|less)$': require.resolve('./css/useable-css-proxy.js'),
    '\\.(css|less)$': 'identity-obj-proxy',
    c3: require.resolve('./helpers/mocking/c3_mock.jsx'),
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': require.resolve('./fileMock.js'),
  },
  testPathIgnorePatterns: [
    '.fixtures.[jt]s$',
  ],
};
