// We are mocking the application version, to avoid failing snapshot tests after a version change.

const mockDefaultExport = {
  getMajorAndMinorVersion: jest.fn(() => 'MAJOR_AND_MINOR_VERSION_MOCK'),
  getFullVersion: jest.fn(() => 'FULL_VERSION_MOCK'),
};

jest.mock('util/Version', () => mockDefaultExport);
