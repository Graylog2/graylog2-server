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

import { Input } from 'components/bootstrap';

class ClosingRetentionStrategyConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    jsonSchema: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  };

  state = {
    max_number_of_indices: this.props.config.max_number_of_indices,
  };

  _onInputUpdate = (field) => {
    return (e) => {
      const update = {};

      update[field] = e.target.value;

      this.setState(update);
      this.props.updateConfig(update);
    };
  };

  render() {
    return (
      <div>
        <fieldset>
          <Input type="number"
                 id="max-number-of-indices"
                 label="Max number of indices"
                 onChange={this._onInputUpdate('max_number_of_indices')}
                 value={this.state.max_number_of_indices}
                 help={<span>Maximum number of indices to keep before <strong>closing</strong> the oldest ones</span>}
                 required />
        </fieldset>
      </div>
    );
  }
}

export default ClosingRetentionStrategyConfiguration;
