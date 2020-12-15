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
import PropTypes from 'prop-types';
import React from 'react';

import { Timestamp } from 'components/common';

class IndexRangeSummary extends React.Component {
  static propTypes = {
    indexRange: PropTypes.object,
  };

  render() {
    const { indexRange } = this.props;

    if (!indexRange) {
      return <span><i>No index range available.</i></span>;
    }

    return (
      <span>Range re-calculated{' '}
        <span title={indexRange.calculated_at}><Timestamp dateTime={indexRange.calculated_at} relative /></span>{' '}
        in {indexRange.took_ms}ms.
      </span>
    );
  }
}

export default IndexRangeSummary;
