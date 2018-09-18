import UniqueChunkIdPlugin from './UniqueChunkIdPlugin';

jest.mock('crypto', () => ({
  randomBytes: () => ({
    toString: () => 'dead',
  }),
}));

const mockCompiler = () => {
  const compilation = {
    hooks: {
      optimizeChunkIds: {
        tap: jest.fn(),
      },
    },
  };

  const compiler = {
    hooks: {
      compilation: {
        tap: jest.fn((_, f) => f(compilation)),
      },
    },
  };

  return { compiler, compilation };
};

describe('UniqueChunkIdPlugin', () => {
  it('register itself properly', () => {
    const uniqueChunkIdPlugin = new UniqueChunkIdPlugin();
    const { compiler, compilation } = mockCompiler();

    uniqueChunkIdPlugin.apply(compiler);

    expect(compiler.hooks.compilation.tap).toHaveBeenCalled();
    expect(compiler.hooks.compilation.tap.mock.calls[0][0]).toEqual('UniqueChunkIdPlugin');
    expect(compilation.hooks.optimizeChunkIds.tap).toHaveBeenCalled();
    expect(compilation.hooks.optimizeChunkIds.tap.mock.calls[0][0]).toEqual('UniqueChunkIdPlugin');
  });

  it('adds random identifier as a prefix for any given chunk', () => {
    const chunks = [{ id: 0, ids: [0] }, { id: 42, ids: [0, 42] }];
    const uniqueChunkIdPlugin = new UniqueChunkIdPlugin();
    const { compiler, compilation } = mockCompiler();

    uniqueChunkIdPlugin.apply(compiler);

    const callback = compilation.hooks.optimizeChunkIds.tap.mock.calls[0][1];
    const result = callback(chunks);

    expect(result).toHaveLength(2);
    expect(result).toMatchSnapshot();
  });

  it('adds random identifier as a prefix for chunks with string ids', () => {
    const chunks = [{ id: 'foo', ids: ['foo'] }, { id: 'bar', ids: ['foo', 'bar'] }];
    const uniqueChunkIdPlugin = new UniqueChunkIdPlugin();
    const { compiler, compilation } = mockCompiler();

    uniqueChunkIdPlugin.apply(compiler);

    const callback = compilation.hooks.optimizeChunkIds.tap.mock.calls[0][1];
    const result = callback(chunks);

    expect(result).toHaveLength(2);
    expect(result).toMatchSnapshot();
  });

  it('does not add random identifier if already present', () => {
    const chunks = [{ id: 'dead-0', ids: ['dead-0'] }, { id: 42, ids: [0, 42] }];
    const uniqueChunkIdPlugin = new UniqueChunkIdPlugin();
    const { compiler, compilation } = mockCompiler();

    uniqueChunkIdPlugin.apply(compiler);

    const callback = compilation.hooks.optimizeChunkIds.tap.mock.calls[0][1];
    const result = callback(chunks);

    expect(result).toHaveLength(2);
    expect(result).toMatchSnapshot();
  });
});
