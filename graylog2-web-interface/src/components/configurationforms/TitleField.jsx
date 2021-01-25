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

import { TextField } from 'components/configurationforms';

class TitleField extends React.Component {
  static propTypes = {
    helpBlock: PropTypes.node,
    onChange: PropTypes.func,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
  };

  static defaultProps = {
    helpBlock: <span />,
    onChange: () => {},
  };

  render() {
    const { typeName } = this.props;
    const titleField = { is_optional: false, attributes: [], human_name: 'Title', description: this.props.helpBlock };

    return (
      <TextField key={`${typeName}-title`}
                 typeName={typeName}
                 title="title"
                 field={titleField}
                 value={this.props.value}
                 onChange={this.props.onChange}
                 autoFocus />
    );
  }
}

export default TitleField;
