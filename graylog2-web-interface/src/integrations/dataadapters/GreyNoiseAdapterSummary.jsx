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

import { Alert } from 'components/bootstrap';

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
    return (
        <div>
          <dl>
            <dt>API Token</dt>
            <dd>******</dd>
          </dl>
          <Alert style={{marginBottom: 10}} bsStyle="danger">
            <h4 style={{marginBottom: 10}}>Deprecation Warning</h4>
            <p>The GreyNoise Community IP Lookup Data Adapter is no longer supported. This Data Adapter should not be used.</p>
          </Alert>
        </div>
    );
  }
}

export default GreyNoiseAdapterSummary;
