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
import lodash from 'lodash';

import { URLWhiteListInput } from 'components/common';
import * as FormsUtils from 'util/FormsUtils';

class HttpNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(config);

    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event) => {
    const { name } = event.target;

    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  static defaultConfig = {
    url: '',
  }

  render() {
    const { config, validation } = this.props;

    return (
      <>
        <URLWhiteListInput label="URL"
                           onChange={this.handleChange}
                           validationState={validation.errors.url ? 'error' : null}
                           validationMessage={lodash.get(validation, 'errors.url[0]', 'The URL to POST to when an Event occurs.')}
                           url={config.url} />
      </>
    );
  }
}

export default HttpNotificationForm;
