const mockResponse = {};

class MockBuilder {
  authenticated = () => this;

  session = () => this;

  setHeader = () => this;

  json = () => this;

  plaintext = () => this;

  noSessionExtension = () => this;

  build = () => Promise.resolve({});
}

class MockFetchError {
}

const MockFetchProvider = Object.assign(
  jest.fn(() => Promise.resolve(mockResponse)),
  {
    FetchError: MockFetchError,
    Builder: MockBuilder,
    default: jest.fn(() => Promise.resolve(mockResponse)),
    fetchPlainText: jest.fn(() => Promise.resolve(mockResponse)),
    fetchPeriodically: jest.fn(() => Promise.resolve(mockResponse)),
    fetchFile: jest.fn(() => Promise.resolve(mockResponse)),
  },
);

jest.mock('logic/rest/FetchProvider', () => MockFetchProvider);
