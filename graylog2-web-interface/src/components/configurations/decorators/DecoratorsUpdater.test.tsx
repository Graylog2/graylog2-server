/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import DecoratorsUpdater from './DecoratorsUpdater';

const mockCreate = jest.fn(() => Promise.resolve());
const mockUpdate = jest.fn(() => Promise.resolve());
const mockRemove = jest.fn(() => Promise.resolve());

jest.mock('injection/CombinedProvider', () => ({
  get: (type) => ({
    Decorators: {
      DecoratorsActions: {
        create: (...args) => mockCreate(...args),
        update: (...args) => mockUpdate(...args),
        remove: (...args) => mockRemove(...args),
      },
    },
  })[type],
}));

const decorator = (id, type = 'dummy') => ({ id, type, order: 0, stream: 'dummystream' });
const newDecorator = (type) => ({ type, order: 0, stream: 'dummystream' });

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
    [decorator('decorator1'), decorator('new1', 'a new decorator'), decorator('decorator3')],
    [decorator('decorator1'), decorator('decorator3')],
  )
    .then(() => {
      expect(mockCreate).toHaveBeenCalledWith(newDecorator('a new decorator'));
      expect(mockUpdate).not.toHaveBeenCalled();
      expect(mockRemove).not.toHaveBeenCalled();
    }));

  it('finds an updated decorator', () => DecoratorsUpdater(
    [decorator('decorator1'), decorator('decorator2', 'other'), decorator('decorator3')],
    [decorator('decorator1'), decorator('decorator2'), decorator('decorator3')],
  )
    .then(() => {
      expect(mockCreate).not.toHaveBeenCalled();
      expect(mockUpdate).toHaveBeenCalledWith('decorator2', decorator('decorator2', 'other'));
      expect(mockRemove).not.toHaveBeenCalled();
    }));

  it('finds a removed decorator', () => DecoratorsUpdater(
    [decorator('decorator1'), decorator('decorator3')],
    [decorator('decorator1'), decorator('decorator2'), decorator('decorator3')],
  )
    .then(() => {
      expect(mockCreate).not.toHaveBeenCalled();
      expect(mockUpdate).not.toHaveBeenCalled();
      expect(mockRemove).toHaveBeenCalledWith('decorator2');
    }));

  it('finds a combination of created, updated & removed decorators', () => DecoratorsUpdater(
    [decorator('decorator2'), decorator('new1', 'new type'), decorator('decorator3', 'bar!'), decorator('new2', 'other new type'), decorator('decorator4', 'something else')],
    [decorator('decorator1'), decorator('decorator2'), decorator('decorator4', 'something'), decorator('decorator3', 'foo!')],
  )
    .then(() => {
      expect(mockCreate).toHaveBeenCalledTimes(2);
      expect(mockCreate).toHaveBeenCalledWith(newDecorator('new type'));
      expect(mockCreate).toHaveBeenCalledWith(newDecorator('other new type'));

      expect(mockUpdate).toHaveBeenCalledTimes(2);
      expect(mockUpdate).toHaveBeenCalledWith('decorator3', decorator('decorator3', 'bar!'));
      expect(mockUpdate).toHaveBeenCalledWith('decorator4', decorator('decorator4', 'something else'));

      expect(mockRemove).toHaveBeenCalledTimes(1);
      expect(mockRemove).toHaveBeenCalledWith('decorator1');
    }));
});
