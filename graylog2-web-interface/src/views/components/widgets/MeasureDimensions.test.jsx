import React from 'react';
import PropTypes from 'prop-types';
import { mount } from 'wrappedEnzyme';

import MeasureDimensions from './MeasureDimensions';

describe('<MeasureDimensions />', () => {
  it('should pass the height of the container to its children', () => {
    class ChildComp extends React.Component {
      static propTypes = {
        containerHeight: PropTypes.number,
      };

      static defaultProps = {
        containerHeight: undefined,
      }

      getContainerHeight() {
        const { containerHeight } = this.props;

        return containerHeight;
      }

      render() {
        return (<div />);
      }
    }
    let childRef = null;
    mount(<MeasureDimensions><ChildComp ref={(node) => { childRef = node; }} /></MeasureDimensions>);

    // JSDOM does not have a height, therefore we can only check that containerHeight was set to 0.
    expect(childRef.getContainerHeight()).toBe(0);
  });
});
