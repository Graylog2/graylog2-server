// We are mocking the application version, to avoid failing snapshot tests after a version change.

const mockDefaultExport = {
  getMajorAndMinorVersion: jest.fn(() => '1.0'),
  getFullVersion: jest.fn(() => '1.0.0-SNAPSHOT'),
};

jest.mock('util/Version', () => mockDefaultExport);
