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

import { MessageField } from 'components/search';
import { MessageDetailsDefinitionList } from 'components/graylog';

class MessageFields extends React.Component {
  static propTypes = {
    customFieldActions: PropTypes.node,
    message: PropTypes.object.isRequired,
    renderForDisplay: PropTypes.func.isRequired,
  };

  static defaultProps = {
    customFieldActions: undefined,
  };

  _formatFields = (fields) => {
    return Object.keys(fields)
      .sort()
      .map((key) => {
        return (
          <MessageField key={key}
                        {...this.props}
                        fieldName={key}
                        value={fields[key]} />
        );
      });
  };

  render() {
    const { message } = this.props;
    const formattedFields = message.formatted_fields;
    const fields = this._formatFields(formattedFields);

    return (
      <MessageDetailsDefinitionList className="message-details-fields">
        {fields}
      </MessageDetailsDefinitionList>
    );
  }
}

export default MessageFields;
