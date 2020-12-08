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

import { TimeUnit } from 'components/common';

class CaffeineCacheSummary extends React.Component {
  static propTypes = {
    cache: PropTypes.object.isRequired,
  };

  render() {
    const { config } = this.props.cache;

    return (
      <dl>
        <dt>Maximum entries</dt>
        <dd>{config.max_size}</dd>
        <dt>Expire after access</dt>
        <dd><TimeUnit value={config.expire_after_access} unit={config.expire_after_access_unit} /></dd>
        <dt>Expire after write</dt>
        <dd><TimeUnit value={config.expire_after_write} unit={config.expire_after_write_unit} /></dd>
      </dl>
    );
  }
}

export default CaffeineCacheSummary;
