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

class BacklogSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
  };

  _formatMessageCount = (count) => {
    if (count === 0) {
      return 'Not including any messages';
    }

    if (count === 1) {
      return 'Including last message';
    }

    return `Including last ${count} messages`;
  };

  render() {
    const { backlog } = this.props.alertCondition.parameters;

    return (
      <span>{this._formatMessageCount(backlog)} in alert notification.</span>
    );
  }
}

export default BacklogSummary;
