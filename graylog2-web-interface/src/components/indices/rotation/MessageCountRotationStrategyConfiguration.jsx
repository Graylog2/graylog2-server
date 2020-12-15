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

class MessageCountRotationStrategyConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    jsonSchema: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  };

  state = {
    max_docs_per_index: this.props.config.max_docs_per_index,
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
                 id="max-docs-per-index"
                 label="Max documents per index"
                 onChange={this._onInputUpdate('max_docs_per_index')}
                 value={this.state.max_docs_per_index}
                 help="Maximum number of documents in an index before it gets rotated"
                 required />
        </fieldset>
      </div>
    );
  }
}

export default MessageCountRotationStrategyConfiguration;
