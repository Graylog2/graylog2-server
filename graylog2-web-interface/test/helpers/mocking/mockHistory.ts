import type { HistoryFunction } from 'routing/useHistory';

const mockHistory = (): HistoryFunction => ({
  goBack: jest.fn(),
  push: jest.fn(),
  pushWithState: jest.fn(),
  replace: jest.fn(),
});

export default mockHistory;
