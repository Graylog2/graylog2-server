const AggregationFunctionsStore = {
  getInitialState: jest.fn(() => ({ avg: undefined, min: undefined, max: undefined })),
  listen: jest.fn(),
};

describe('SeriesFunctionsSuggester', () => {
  let SeriesFunctionsSuggester;
  beforeEach(() => {
    jest.doMock('views/stores/AggregationFunctionsStore', () => AggregationFunctionsStore);

    // eslint-disable-next-line global-require
    SeriesFunctionsSuggester = require('./SeriesFunctionsSuggester');
  });
  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
  });
  it('returns default functions', () => {
    const suggester = new SeriesFunctionsSuggester();
    expect(suggester.defaults).toMatchSnapshot();
  });

  it('completes functions with field names', () => {
    const suggester = new SeriesFunctionsSuggester(['action', 'controller', 'took_ms']);
    expect(suggester.for('avg')).toMatchSnapshot();
  });

  it('does not complete functions without field names', () => {
    const suggester = new SeriesFunctionsSuggester([]);
    expect(suggester.for('avg')).toEqual([]);
  });

  it('updates functions when triggered', () => {
    const suggester = new SeriesFunctionsSuggester(['action', 'controller', 'took_ms']);

    expect(AggregationFunctionsStore.listen).toHaveBeenCalled();
    expect(AggregationFunctionsStore.getInitialState).toHaveBeenCalled();
    expect(suggester.defaults).toHaveLength(4);
    expect(suggester.defaults).toMatchSnapshot('default functions before update');

    const callback = AggregationFunctionsStore.listen.mock.calls[0][0];

    const newFunctions = { card: undefined, stddev: undefined };
    callback(newFunctions);

    expect(suggester.defaults).toHaveLength(3);
    expect(suggester.defaults).toMatchSnapshot('default functions after update');
  });
});
