/* eslint-disable react/no-multi-comp */
import React from 'react';
import { mount } from 'enzyme';
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

class SimpleComponentWithDummyStore extends React.Component {
  static propTypes = {
    simpleStore: PropTypes.shape({ foo: PropTypes.number }),
  };
  static defaultProps = {
    simpleStore: undefined,
  };

  render() {
    const { simpleStore } = this.props;
    if (simpleStore && simpleStore.value) {
      return <span>Value is: {simpleStore.value}</span>;
    }
    return <span>No value.</span>;
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
});
