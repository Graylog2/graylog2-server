/* eslint-disable react/no-multi-comp */
import React from 'react';
import renderer from 'react-test-renderer';
import Reflux from 'reflux';
import PropTypes from 'prop-types';

import connect from './connect';

class SimpleComponentWithoutStores extends React.Component {
  render() {
    return <span>Hello World!</span>;
  }
}

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
    const wrapper = renderer.create(<Component />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('connects component to store without state', () => {
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    const wrapper = renderer.create(<Component />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('connects component to store with state', () => {
    SimpleStore.setValue(42);
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore });
    const wrapper = renderer.create(<Component />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('renders spinner when store is not ready', () => {
    SimpleStore.reset();
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore }, [], true);
    const wrapper = renderer.create(<Component />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('executes action on store upon mount', () => {
    SimpleStore.reset();
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore }, [() => SimpleStore.setValue(23)]);
    const wrapper = renderer.create(<Component />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('waits on store per default before rendering component when more than zero actions are passed', () => {
    SimpleStore.reset();
    const Component = connect(SimpleComponentWithDummyStore, { simpleStore: SimpleStore }, [SimpleStore.noop]);
    const wrapper = renderer.create(<Component />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
