import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { mount } from 'wrappedEnzyme';

import MeasureDimensions from './MeasureDimensions';

describe('<MeasureDimensions />', () => {
  it('should pass the height of the container to its children', () => {
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

    // JSDOM does not have a height, therefore we can only check that containerHeight was set to 0.
    expect(childRef.getContainerHeight()).toBe(0);
  });
});
