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
