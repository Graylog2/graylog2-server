// @flow strict
import DecoratorsUpdater from './DecoratorsUpdater';

const mockCreate = jest.fn(() => Promise.resolve());
const mockUpdate = jest.fn(() => Promise.resolve());
const mockRemove = jest.fn(() => Promise.resolve());

jest.mock('injection/CombinedProvider', () => ({
  get: type => ({
    Decorators: {
      DecoratorsActions: {
        create: (...args) => mockCreate(...args),
        update: (...args) => mockUpdate(...args),
        remove: (...args) => mockRemove(...args),
      },
    },
  })[type],
}));

describe('DecoratorsUpdater', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });
  it('works for empty arrays', () => DecoratorsUpdater([], [])
    .then(() => {
      expect(mockCreate).not.toHaveBeenCalled();
      expect(mockUpdate).not.toHaveBeenCalled();
      expect(mockRemove).not.toHaveBeenCalled();
    }));
  it('finds a created decorator', () => DecoratorsUpdater(
    [{ id: 'decorator1' }, { value: 42 }, { id: 'decorator3' }],
    [{ id: 'decorator1' }, { id: 'decorator3' }],
  )
    .then(() => {
      expect(mockCreate).toHaveBeenCalledWith({ value: 42 });
      expect(mockUpdate).not.toHaveBeenCalled();
      expect(mockRemove).not.toHaveBeenCalled();
    }));
  it('finds an updated decorator', () => DecoratorsUpdater(
    [{ id: 'decorator1' }, { id: 'decorator2', value: 42 }, { id: 'decorator3' }],
    [{ id: 'decorator1' }, { id: 'decorator2', value: 23 }, { id: 'decorator3' }],
  )
    .then(() => {
      expect(mockCreate).not.toHaveBeenCalled();
      expect(mockUpdate).toHaveBeenCalledWith('decorator2', { id: 'decorator2', value: 42 });
      expect(mockRemove).not.toHaveBeenCalled();
    }));
  it('finds a removed decorator', () => DecoratorsUpdater(
    [{ id: 'decorator1' }, { id: 'decorator3' }],
    [{ id: 'decorator1' }, { id: 'decorator2' }, { id: 'decorator3' }],
  )
    .then(() => {
      expect(mockCreate).not.toHaveBeenCalled();
      expect(mockUpdate).not.toHaveBeenCalled();
      expect(mockRemove).toHaveBeenCalledWith('decorator2');
    }));

  it('finds a combination of created, updated & removed decorators', () => DecoratorsUpdater(
    [{ id: 'decorator2' }, { value: 23 }, { id: 'decorator3', value: 'bar!' }, { value: 42 }, { id: 'decorator4', something: 'something else' }],
    [{ id: 'decorator1' }, { id: 'decorator2' }, { id: 'decorator4', something: 'something' }, { id: 'decorator3', value: 'foo!' }],
  )
    .then(() => {
      expect(mockCreate).toHaveBeenCalledTimes(2);
      expect(mockCreate).toHaveBeenCalledWith({ value: 23 });
      expect(mockCreate).toHaveBeenCalledWith({ value: 42 });

      expect(mockUpdate).toHaveBeenCalledTimes(2);
      expect(mockUpdate).toHaveBeenCalledWith('decorator3', { id: 'decorator3', value: 'bar!' });
      expect(mockUpdate).toHaveBeenCalledWith('decorator4', { id: 'decorator4', something: 'something else' });

      expect(mockRemove).toHaveBeenCalledTimes(1);
      expect(mockRemove).toHaveBeenCalledWith('decorator1');
    }));
});
