// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';
import Reflux from 'reflux';
import PropTypes from 'prop-types';
import { Map, List } from 'immutable';

import { asMock } from 'helpers/mocking/index';
import connect from './connect';

const SimpleComponentWithoutStores = () => <span>Hello World!</span>;

const SimpleStore = Reflux.createStore({
  getInitialState() {
    return this.state;
  },
  setValue(value) {
    this.state = { value: value };
    this.trigger(this.state);
  },
  noop() {},
  reset() {
    this.state = undefined;
    this.trigger(this.state);
  },
});

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

class AlwaysEqual {
  equals(other) {
    return true;
  }
}

class NeverEqual {
  equals(other) {
    return false;
  }
}

class NonValueClass {
  value: number;

  constructor(value: number) {
    this.value = value;
  }
}

describe('connect()', () => {
  it('does not do anything if no stores are provided', () => {
    const Component = connect(SimpleComponentWithoutStores, {});
    const wrapper = mount(<Component />);
    expect(wrapper).toHaveHTML('<span>Hello World!</span>');
  });

  it('connects component to store without state', () => {
    SimpleStore.reset();
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
    SimpleStore.reset();
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

  describe('generates `shouldComponentUpdate`', () => {
    const Component = () => <span>Hello!</span>;
    const SimplestStore = ({
      getInitialState: jest.fn(),
      listen: jest.fn(() => () => {}),
    });
    it('comparing empty values properly', () => {
      const ComponentClass = connect(Component, {});
      const component = new ComponentClass({});

      expect(component.shouldComponentUpdate({}, {})).toBeFalsy();
    });
    const verifyShouldComponentUpdate = ({ initial, next, result }) => {
      asMock(SimplestStore.getInitialState).mockReturnValue(initial);
      const ComponentClass = connect(Component, { foo: SimplestStore });

      const component = new ComponentClass({});
      expect(component.shouldComponentUpdate({}, { foo: next })).toBe(result);

      const componentWithProps = new ComponentClass({ someProp: initial });
      expect(componentWithProps.shouldComponentUpdate({ someProp: next }, { foo: initial })).toBe(result);
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
  `('compares $description and returns $result', verifyShouldComponentUpdate);
  });
});
