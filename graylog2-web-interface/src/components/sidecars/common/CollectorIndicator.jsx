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
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import OperatingSystemIcon from './OperatingSystemIcon';

const CollectorIndicator = createReactClass({
  propTypes: {
    collector: PropTypes.string.isRequired,
    operatingSystem: PropTypes.string,
  },

  getDefaultProps() {
    return {
      operatingSystem: undefined,
    };
  },

  render() {
    const { collector, operatingSystem } = this.props;

    return (
      <span>
        <OperatingSystemIcon operatingSystem={operatingSystem} /> {collector}
        {operatingSystem && <span> on {lodash.upperFirst(operatingSystem)}</span>}
      </span>
    );
  },
});

export default CollectorIndicator;
