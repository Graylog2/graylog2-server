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
import React from 'react';
import { act, render, screen } from 'wrappedTestingLibrary';
import Reflux from 'reflux';
import PropTypes from 'prop-types';
import { Map, List } from 'immutable';

import { asMock } from 'helpers/mocking/index';

import {
  arrayOfMaps,
  listWithObject,
  mapWithObject,
  mixedMapsAndObjects,
  objectWithMap,
  AlwaysEqual,
  NeverEqual,
  NonValueClass,
} from './EqualityCheck.fixtures';

import connect, { useStore } from '../connect';

const SimpleComponentWithoutStores = () => <span>Hello World!</span>;
const createSimpleStore = () => Reflux.createStore<{ value: number }>({
  getInitialState() {
    return this.state;
  },
  setValue(value: number) {
    this.state = { value };
    this.trigger(this.state);
  },
  noop() {},
  reset() {
    this.state = undefined;
    this.trigger(this.state);
  },
});

const SimpleStore = createSimpleStore();

const SimpleComponentWithDummyStore = ({ simpleStore }) => {
  if (simpleStore && simpleStore.value) {
    return <span>Value is: {simpleStore.value}</span>;
  }

  return <span>No value.</span>;
};

SimpleComponentWithDummyStore.propTypes = {
  simpleStore: PropTypes.shape({
    value: PropTypes.number,
  }),
};

SimpleComponentWithDummyStore.defaultProps = {
  simpleStore: undefined,
};

describe('connect()', () => {
  beforeEach(() => {
    act(() => {
      SimpleStore.reset();
    });
  });

  it('does not do anything if no stores are provided', async () => {
    const Component = connect(SimpleComponentWithoutStores, {});
    render(<Component />);

    await screen.findByText('Hello World!');
  });

  it('connects component to store without state', async () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    render(<Component />);

    await screen.findByText('No value.');
  });

  it('connects component to store with state', async () => {
    act(() => {
      SimpleStore.setValue(42);
    });

    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    render(<Component />);

    await screen.findByText('Value is: 42');
  });

  it('reflects state changes in store', async () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    render(<Component />);

    await screen.findByText('No value.');

    act(() => {
      SimpleStore.setValue(42);
    });

    await screen.findByText('Value is: 42');

    SimpleStore.noop();

    await screen.findByText('Value is: 42');

    act(() => {
      SimpleStore.reset();
    });

    await screen.findByText('No value.');
  });

  it('allows mangling of props before passing them', async () => {
    const Component = connect(
      SimpleComponentWithDummyStore,
      { simpleStore: SimpleStore },
      ({ simpleStore }) => (simpleStore && { simpleStore: { value: simpleStore.value * 2 } }),
    );
    render(<Component />);

    act(() => {
      SimpleStore.setValue(42);
    });

    await screen.findByText('Value is: 84');
  });

  it('adds meaningful name to wrapper component', () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });

    expect(Component.displayName).toEqual('ConnectStoresWrapper[SimpleComponentWithDummyStore] stores=simpleStore');
  });

  it('does not fail when anonymous component is passed', () => {
    const Component = connect(() => <span>hello!</span>, { simpleStore: SimpleStore });

    expect(Component.displayName).toEqual('ConnectStoresWrapper[Unknown/Anonymous] stores=simpleStore');
  });

  it('types store props as optional', async () => {
    const Component = connect(() => <span>hello!</span>, { simpleStore: SimpleStore });
    render(<Component />);
    await screen.findByText('hello!');
  });

  it('types mapped props as optional', async () => {
    const Component = connect(
      () => <span>hello!</span>,
      { simpleStore: SimpleStore },
      ({ simpleStore }) => (simpleStore && { storeValue: simpleStore.value }),
    );
    render(<Component />);
    await screen.findByText('hello!');
  });

  it('types props which have a default value (defaultProps) as optional', async () => {
    const BaseComponent = ({ exampleProp }: {
      exampleProp: string
    }) => <span>{exampleProp}</span>;

    BaseComponent.defaultProps = {
      exampleProp: 'hello!',
    };

    BaseComponent.propTypes = {
      exampleProp: PropTypes.string,
    };

    const Component = connect(BaseComponent, { simpleStore: SimpleStore });
    render(<Component />);

    await screen.findByText('hello!');
  });

  describe('generates `shouldComponentUpdate`', () => {
    const Component: React.ComponentType<{ someProp?: any, foo: number }> = jest.fn(() => <span>Hello!</span>);

    afterEach(() => { jest.clearAllMocks(); });

    it('comparing empty values properly', async () => {
      const ComponentClass = connect(Component, {});

      const { rerender } = render(<ComponentClass foo={42} />);

      // @ts-expect-error
      rerender(<ComponentClass />);

      await screen.findByText('Hello!');
    });

    const verifyShouldComponentUpdate = ({ initial, next, result }) => {
      SimpleStore.setValue(initial);
      const ComponentClass = connect(Component, { foo: SimpleStore });

      render(<ComponentClass />);

      asMock(Component).mockClear();

      act(() => {
        SimpleStore.setValue(next);
      });

      if (result) {
        expect(Component).toHaveBeenCalled();
      } else {
        expect(Component).not.toHaveBeenCalled();
      }
    };

    // eslint-disable-next-line jest/expect-expect
    it.each`
    initial                  | next                     | result    | description
    ${undefined}             | ${undefined}             | ${false}  | ${'equal undefined values'}
    ${undefined}             | ${null}                  | ${true}   | ${'from undefined to null value'}
    ${undefined}             | ${42}                    | ${true}   | ${'from undefined to numeric value'}
    ${42}                    | ${42}                    | ${false}  | ${'equal numeric values'}
    ${42}                    | ${23}                    | ${true}   | ${'non-equal numeric values'}
    ${'Hello there!'}        | ${'Hello there!'}        | ${false}  | ${'equal string values'}
    ${'Hello there!'}        | ${'Hello World!'}        | ${true}   | ${'non-equal string values'}
    ${{}}                    | ${{}}                    | ${false}  | ${'equal empty objects'}
    ${{ bar: 23 }}           | ${{ bar: 23 }}           | ${false}  | ${'equal objects'}
    ${{ bar: 23 }}           | ${{ bar: 42 }}           | ${true}   | ${'non-equal objects'}
    ${[]}                    | ${[]}                    | ${false}  | ${'equal empty arrays'}
    ${[23]}                  | ${[23]}                  | ${false}  | ${'equal arrays'}
    ${[23]}                  | ${[42]}                  | ${true}   | ${'non-equal arrays'}
    ${Map()}                 | ${Map()}                 | ${false}  | ${'equal empty immutable maps'}
    ${Map({ bar: 23 })}      | ${Map({ bar: 23 })}      | ${false}  | ${'equal immutable maps'}
    ${Map({ bar: 23 })}      | ${Map({ bar: 42 })}      | ${true}   | ${'non-equal immutable maps'}
    ${List()}                | ${List()}                | ${false}  | ${'equal empty immutable lists'}
    ${List([23])}            | ${List([23])}            | ${false}  | ${'equal immutable lists'}
    ${List([23])}            | ${List([42])}            | ${true}   | ${'non-equal immutable lists'}
    ${new AlwaysEqual()}     | ${new AlwaysEqual()}     | ${false}  | ${'value class which is always equal'}
    ${new NeverEqual()}      | ${new NeverEqual()}      | ${true}   | ${'value class which is never equal'}
    ${new AlwaysEqual()}     | ${new NeverEqual()}      | ${false}  | ${'value class which is always equal'}
    ${new NeverEqual()}      | ${new AlwaysEqual()}     | ${true}   | ${'value class which is never equal'}
    ${new NonValueClass(23)} | ${new NonValueClass(42)} | ${true}   | ${'value class which is never equal'}
    ${mapWithObject()}       | ${mapWithObject()}       | ${false}  | ${'immutable maps containing objects'}
    ${listWithObject()}      | ${listWithObject()}      | ${false}  | ${'immutable lists containing objects'}
    ${objectWithMap()}       | ${objectWithMap()}       | ${false}  | ${'objects containing immutable maps'}
    ${arrayOfMaps()}         | ${arrayOfMaps()}         | ${false}  | ${'arrays containing immutable maps'}
    ${mixedMapsAndObjects()} | ${mixedMapsAndObjects()} | ${false}  | ${'nested immutable maps and objects'}
  `('compares $description and returns $result', verifyShouldComponentUpdate);
  });
});

describe('useStore', () => {
  const SimpleComponent = () => {
    const { value } = useStore(SimpleStore) || {};

    return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
  };

  beforeEach(() => {
    act(() => SimpleStore.reset());
  });

  it('renders state from store', async () => {
    render(<SimpleComponent />);

    await screen.findByText('No value.');
  });

  it('connects component to store with state', async () => {
    act(() => SimpleStore.setValue(42));
    render(<SimpleComponent />);

    await screen.findByText('Value is: 42');
  });

  it('reflects state changes from store', async () => {
    render(<SimpleComponent />);

    await screen.findByText('No value.');

    act(() => SimpleStore.setValue(42));

    await screen.findByText('Value is: 42');

    act(() => SimpleStore.noop());

    await screen.findByText('Value is: 42');

    act(() => SimpleStore.reset());

    await screen.findByText('No value.');
  });

  it('allows mangling of props before passing them', async () => {
    const Component = () => {
      const { value } = useStore(SimpleStore, ({ value: v } = { value: 0 }) => ({ value: v * 2 })) || {};

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    render(<Component />);

    await screen.findByText('No value.');

    act(() => SimpleStore.setValue(42));

    await screen.findByText('Value is: 84');
  });

  it('does not rerender component if state does not change', () => {
    let renderCount = 0;

    const SimpleComponentWithRenderCounter = () => {
      renderCount += 1;
      const { value } = useStore(SimpleStore) || {};

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    render(<SimpleComponentWithRenderCounter />);

    const beforeFirstSet = renderCount;

    act(() => SimpleStore.setValue(42));

    expect(renderCount).toEqual(beforeFirstSet + 1);

    const beforeSecondSet = renderCount;

    act(() => SimpleStore.setValue(42));

    expect(renderCount).toEqual(beforeSecondSet);
  });

  it('does not reregister if props mapper is provided as arrow function', async () => {
    const listenSpy = jest.spyOn(SimpleStore, 'listen');

    const ComponentWithPropsMapper = ({ propsMapper }: { propsMapper: (state: { value: number }) => ({ value: number }) }) => {
      const { value } = useStore(SimpleStore, propsMapper) || { value: undefined };

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    const { rerender } = render(<ComponentWithPropsMapper propsMapper={(x) => x} />);

    await screen.findByText('No value.');

    rerender(<ComponentWithPropsMapper propsMapper={({ value: v } = { value: 0 }) => ({ value: v * 2 })} />);
    act(() => SimpleStore.setValue(42));

    await screen.findByText('Value is: 84');

    expect(listenSpy).toHaveBeenCalledTimes(1);
  });
});
