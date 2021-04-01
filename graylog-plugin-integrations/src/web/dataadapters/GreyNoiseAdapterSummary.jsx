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
// eslint-disable-next-line react/prefer-stateless-function
import React from 'react';
import PropTypes from 'prop-types';

class GreyNoiseAdapterSummary extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.shape({
      config: PropTypes.shape({
      }).isRequired,
      updateConfig: PropTypes.func.isRequired,
      handleFormEvent: PropTypes.func.isRequired,
      validationState: PropTypes.func.isRequired,
      validationMessage: PropTypes.func.isRequired,
    }),
  };

  render() {
    const { config } = this.props.dataAdapter;

    return (
      <dl>
        <dt>API Token</dt>
        <dd>******</dd>
      </dl>
    );
  }
}

export default GreyNoiseAdapterSummary;
