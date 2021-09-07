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
/// <reference types="jest-enzyme" />
import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { mount } from 'wrappedEnzyme';
import Reflux from 'reflux';
import PropTypes from 'prop-types';
import { Map, List } from 'immutable';
import { asMock } from 'helpers/mocking/index';

import type { Store } from 'stores/StoreTypes';

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

const createSimpleStore = () => Reflux.createStore<{ value: number }>({
  getInitialState() {
    return this.state;
  },
  setValue(value) {
    this.state = { value };
    this.trigger(this.state);
  },
  noop() {},
  reset() {
    this.state = undefined;
    this.trigger(this.state);
  },
});

describe('connect()', () => {
  const SimpleComponentWithoutStores = () => <span>Hello World!</span>;

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

  let SimpleStore: ReturnType<typeof createSimpleStore>;

  beforeEach(() => {
    SimpleStore = createSimpleStore();
  });

  it('does not do anything if no stores are provided', () => {
    const Component = connect(SimpleComponentWithoutStores, {});
    const wrapper = mount(<Component />);

    expect(wrapper).toHaveHTML('<span>Hello World!</span>');
  });

  it('connects component to store without state', () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    const wrapper = mount(<Component />);

    expect(wrapper).toHaveText('No value.');
  });

  it('connects component to store with state', () => {
    SimpleStore.setValue(42);
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    const wrapper = mount(<Component />);

    expect(wrapper).toHaveText('Value is: 42');
  });

  it('reflects state changes in store', () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    const wrapper = mount(<Component />);

    expect(wrapper).toHaveText('No value.');

    SimpleStore.setValue(42);

    expect(wrapper).toHaveText('Value is: 42');

    SimpleStore.noop();

    expect(wrapper).toHaveText('Value is: 42');

    SimpleStore.reset();

    expect(wrapper).toHaveText('No value.');
  });

  it('allows mangling of props before passing them', () => {
    const Component = connect(
      SimpleComponentWithDummyStore,
      { simpleStore: SimpleStore },
      ({ simpleStore }) => (simpleStore && { simpleStore: { value: simpleStore.value * 2 } }),
    );
    const wrapper = mount(<Component />);

    SimpleStore.setValue(42);

    expect(wrapper).toHaveText('Value is: 84');
  });

  it('adds meaningful name to wrapper component', () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });

    expect(Component.displayName).toEqual('ConnectStoresWrapper[SimpleComponentWithDummyStore] stores=simpleStore');
  });

  it('does not fail when anonymous component is passed', () => {
    const Component = connect(() => <span>hello!</span>, { simpleStore: SimpleStore });

    expect(Component.displayName).toEqual('ConnectStoresWrapper[Unknown/Anonymous] stores=simpleStore');
  });

  // eslint-disable-next-line jest/expect-expect
  it('types store props as optional', () => {
    const Component = connect(() => <span>hello!</span>, { simpleStore: SimpleStore });
    mount(<Component />);
  });

  // eslint-disable-next-line jest/expect-expect
  it('types mapped props as optional', () => {
    const Component = connect(
      () => <span>hello!</span>,
      { simpleStore: SimpleStore },
      ({ simpleStore }) => (simpleStore && { storeValue: simpleStore.value }),
    );
    mount(<Component />);
  });

  // eslint-disable-next-line jest/expect-expect
  it('types props which have a default value (defaultProps) as optional', () => {
    const BaseComponent = ({ exampleProp }: { exampleProp: string }) => <span>{exampleProp}</span>;

    BaseComponent.defaultProps = {
      exampleProp: 'hello!',
    };

    BaseComponent.propTypes = {
      exampleProp: PropTypes.string,
    };

    const Component = connect(BaseComponent, { simpleStore: SimpleStore });
    mount(<Component />);
  });

  describe('generates `shouldComponentUpdate`', () => {
    const Component: React.ComponentType<{ someProp?: any, foo: number }> = jest.fn(() => <span>Hello!</span>);
    const SimplestStore: Store<number> = ({
      getInitialState: jest.fn(() => 42),
      listen: jest.fn(() => () => {}),
    });

    afterEach(() => { jest.clearAllMocks(); });

    it('comparing empty values properly', () => {
      const ComponentClass = connect(Component, {});

      const wrapper = mount(<ComponentClass foo={42} />);

      wrapper.setProps({});

      expect(Component).toHaveBeenCalledTimes(1);
    });

    const verifyShouldComponentUpdate = ({ initial, next, result }) => {
      asMock(SimplestStore.getInitialState).mockReturnValue(initial);
      const ComponentClass = connect(Component, { foo: SimplestStore });

      mount(<ComponentClass />);
      const update = asMock(SimplestStore.listen).mock.calls[0][0];

      asMock(Component).mockClear();

      update(next);

      if (result) {
        expect(Component).toHaveBeenCalled();
      } else {
        expect(Component).not.toHaveBeenCalled();
      }

      const wrapper = mount(<ComponentClass someProp={initial} />);

      asMock(Component).mockClear();

      wrapper.setProps({ someProp: next });

      if (result) {
        expect(Component).toHaveBeenCalled();
      } else {
        expect(Component).not.toHaveBeenCalled();
      }
    };

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
  let SimpleStore: ReturnType<typeof createSimpleStore>;

  const SimpleComponent = () => {
    const { value } = useStore(SimpleStore) || {};

    return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
  };

  beforeEach(() => {
    SimpleStore = createSimpleStore();
    jest.clearAllMocks();
  });

  it('renders state from store', () => {
    const wrapper = mount(<SimpleComponent />);

    expect(wrapper).toHaveText('No value.');
  });

  it('connects component to store with state', () => {
    act(() => SimpleStore.setValue(42));
    const wrapper = mount(<SimpleComponent />);

    expect(wrapper).toHaveText('Value is: 42');
  });

  it('reflects state changes from store', () => {
    const wrapper = mount(<SimpleComponent />);

    expect(wrapper).toHaveText('No value.');

    act(() => SimpleStore.setValue(42));

    expect(wrapper).toHaveText('Value is: 42');

    act(() => SimpleStore.noop());

    expect(wrapper).toHaveText('Value is: 42');

    act(() => SimpleStore.reset());

    expect(wrapper).toHaveText('No value.');
  });

  it('allows mangling of props before passing them', () => {
    const Component = () => {
      const { value } = useStore(SimpleStore, ({ value: v } = { value: 0 }) => ({ value: v * 2 })) || {};

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    const wrapper = mount(<Component />);

    expect(wrapper).toHaveText('No value.');

    act(() => SimpleStore.setValue(42));

    expect(wrapper).toHaveText('Value is: 84');
  });

  it('does not rerender component if state does not change', () => {
    let renderCount = 0;

    const SimpleComponentWithRenderCounter = () => {
      renderCount += 1;
      const { value } = useStore(SimpleStore) || {};

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    mount(<SimpleComponentWithRenderCounter />);

    const beforeFirstSet = renderCount;

    act(() => SimpleStore.setValue(42));

    expect(renderCount).toEqual(beforeFirstSet + 1);

    const beforeSecondSet = renderCount;

    act(() => SimpleStore.setValue(42));

    expect(renderCount).toEqual(beforeSecondSet);
  });

  it('does not reregister if same props mapper is provided', () => {
    const listenSpy = jest.spyOn(SimpleStore, 'listen');
    const _propsMapper = (x) => x;

    const ComponentWithPropsMapper = ({ propsMapper }: { propsMapper: (state: { value: number }) => ({ value: number }) }) => {
      const { value } = useStore(SimpleStore, propsMapper) || { value: undefined };

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    expect(listenSpy).toHaveBeenCalledTimes(0);

    const wrapper = mount(<ComponentWithPropsMapper propsMapper={_propsMapper} />);

    expect(wrapper).toHaveText('No value.');

    wrapper.setProps({ propsMapper: _propsMapper });
    act(() => SimpleStore.setValue(42));

    expect(wrapper).toHaveText('Value is: 42');

    expect(listenSpy).toHaveBeenCalledTimes(1);
  });

  it('reretrieves store state when props mapper function changes', () => {
    act(() => SimpleStore.setValue(42));

    const ComponentWithPropsMapper = ({ propsMapper }: { propsMapper: (state: { value: number }) => ({ value: number }) }) => {
      const { value } = useStore(SimpleStore, propsMapper) || { value: undefined };

      return <span>{value ? `Value is: ${value}` : 'No value.'}</span>;
    };

    const wrapper = mount(<ComponentWithPropsMapper propsMapper={(x) => x} />);

    expect(wrapper).toHaveText('Value is: 42');

    wrapper.setProps({ propsMapper: ({ value: v } = { value: 0 }) => ({ value: v * 2 }) });

    expect(wrapper).toHaveText('Value is: 84');
  });

  it('does not rerender component if mapped state is constant', () => {
    act(() => SimpleStore.setValue(3));

    const ComponentWithPropsMapper = () => {
      const value = useStore(SimpleStore, (state) => (state.value % 2 === 0 ? 'even' : 'odd'));

      return <span>Value is: {value}</span>;
    };

    const onRender = jest.fn();

    const wrapper = mount(<React.Profiler id="useStore" onRender={onRender}><ComponentWithPropsMapper /></React.Profiler>);

    expect(wrapper).toHaveText('Value is: odd');

    act(() => SimpleStore.setValue(9));

    expect(wrapper).toHaveText('Value is: odd');

    expect(onRender).toHaveBeenCalledTimes(1);

    act(() => SimpleStore.setValue(6));

    expect(wrapper).toHaveText('Value is: even');

    expect(onRender).toHaveBeenCalledTimes(2);
  });

  it('allows connecting states of different stores', () => {
    const AnotherSimpleStore = createSimpleStore();

    act(() => SimpleStore.setValue(2));
    act(() => AnotherSimpleStore.setValue(3));

    const ComponentWithPropsMapper = () => {
      const value1 = useStore(SimpleStore, (state) => state?.value ?? 0);
      const value2 = useStore(AnotherSimpleStore, (state) => (state?.value ?? 0) * value1);

      return <span>Value is: {value2}</span>;
    };

    const wrapper = mount(<ComponentWithPropsMapper />);

    expect(wrapper).toHaveText('Value is: 6');

    act(() => SimpleStore.setValue(3));

    expect(wrapper).toHaveText('Value is: 9');
  });
});
