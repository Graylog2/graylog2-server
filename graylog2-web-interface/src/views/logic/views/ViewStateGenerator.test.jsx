// @flow strict
import View from './View';
import ViewStateGenerator from './ViewStateGenerator';
import MessagesWidget from '../widgets/MessagesWidget';

const mockList = jest.fn(() => Promise.resolve([]));
jest.mock('injection/CombinedProvider', () => ({
  get: type => ({
    Decorators: {
      DecoratorsActions: {
        list: (...args) => mockList(...args),
      },
    },
  })[type],
}));

describe('ViewStateGenerator', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });
  it('adds message table to widgets', async () => {
    const result = await ViewStateGenerator(View.Type.Search);
    const messageTableWidget = result.widgets.find(widget => widget.type === MessagesWidget.type);
    expect(messageTableWidget).toBeDefined();
  });
  it('adds decorators for current stream to message table', async () => {
    mockList.mockReturnValue([
      { id: 'decorator1', stream: 'foobar', order: 0, type: 'something' },
      { id: 'decorator2', stream: 'different', order: 0, type: 'something' },
    ]);
    const result = await ViewStateGenerator(View.Type.Search, 'foobar');

    expect(mockList).toHaveBeenCalledWith();
    const messageTableWidget = result.widgets.find(widget => widget.type === MessagesWidget.type);
    expect(messageTableWidget.config.decorators).toEqual([{ id: 'decorator1', stream: 'foobar', order: 0, type: 'something' }]);
  });
  it('adds decorators for default search to message table if stream id is `null`', async () => {
    mockList.mockReturnValue([
      { id: 'decorator1', stream: 'foobar', order: 0, type: 'something' },
      { id: 'decorator2', stream: null, order: 0, type: 'something' },
    ]);
    const result = await ViewStateGenerator(View.Type.Search, null);

    expect(mockList).toHaveBeenCalledWith();
    const messageTableWidget = result.widgets.find(widget => widget.type === MessagesWidget.type);
    expect(messageTableWidget.config.decorators).toEqual([{ id: 'decorator2', stream: null, order: 0, type: 'something' }]);
  });
  it('does not add decorators for current stream to message table if none exist for this stream', async () => {
    mockList.mockReturnValue([
      { id: 'decorator1', stream: 'foobar', order: 0, type: 'something' },
      { id: 'decorator2', stream: null, order: 0, type: 'something' },
    ]);
    const result = await ViewStateGenerator(View.Type.Search, 'otherstream');

    expect(mockList).toHaveBeenCalledWith();
    const messageTableWidget = result.widgets.find(widget => widget.type === MessagesWidget.type);
    expect(messageTableWidget.config.decorators).toEqual([]);
  });
  it('does not add decorators for current stream to message table if none exist at all', async () => {
    const result = await ViewStateGenerator(View.Type.Search, 'otherstream');

    expect(mockList).toHaveBeenCalledWith();
    const messageTableWidget = result.widgets.find(widget => widget.type === MessagesWidget.type);
    expect(messageTableWidget.config.decorators).toEqual([]);
  });
  it('does not add decorators for default search to message table if none exist at all', async () => {
    const result = await ViewStateGenerator(View.Type.Search, null);

    expect(mockList).toHaveBeenCalledWith();
    const messageTableWidget = result.widgets.find(widget => widget.type === MessagesWidget.type);
    expect(messageTableWidget.config.decorators).toEqual([]);
  });
});
