import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { mount } from 'enzyme';

import MeasureDimensions from './MeasureDimensions';

describe('<MeasureDimensions />', () => {
  it('should pass a function to getHeight of Container', () => {
    const ChildComp = createReactClass({
      propTypes: {
        containerHeight: PropTypes.number,
      },

      getDefaultProps() {
        return {
          containerHeight: undefined,
        };
      },

      getContainerHeight() {
        return this.props.containerHeight;
      },

      render() {
        return (<div />);
      },
    });
    let childRef = null;
    mount(<MeasureDimensions><ChildComp ref={(node) => { childRef = node; }} /></MeasureDimensions>);
    expect(childRef.getContainerHeight()).not.toBe(undefined);
  });
});
