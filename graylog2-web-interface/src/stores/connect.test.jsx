/* eslint-disable react/no-multi-comp */
import React from 'react';
import { mount } from 'wrappedEnzyme';
import Reflux from 'reflux';
import PropTypes from 'prop-types';

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
});
