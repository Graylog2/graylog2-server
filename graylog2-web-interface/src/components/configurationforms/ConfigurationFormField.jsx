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

import {
  BooleanField,
  DropdownField,
  ListField,
  NumberField,
  TextField,
} from 'components/configurationforms';

export default class ConfigurationFormField extends React.Component {
  static propTypes = {
    typeName: PropTypes.string.isRequired,
    configField: PropTypes.object.isRequired,
    configKey: PropTypes.string.isRequired,
    configValue: PropTypes.any,
    autoFocus: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
  };

  static defaultProps = {
    configValue: undefined,
    autoFocus: false,
  };

  render() {
    const { typeName, configField, configKey, configValue, autoFocus, onChange } = this.props;
    const elementKey = `${typeName}-${configKey}`;

    switch (configField.type) {
      case 'text':
        return (
          <TextField key={elementKey}
                     typeName={typeName}
                     title={configKey}
                     field={configField}
                     value={configValue}
                     onChange={onChange}
                     autoFocus={autoFocus} />
        );
      case 'number':
        return (
          <NumberField key={elementKey}
                       typeName={typeName}
                       title={configKey}
                       field={configField}
                       value={configValue}
                       onChange={onChange}
                       autoFocus={autoFocus} />
        );
      case 'boolean':
        return (
          <BooleanField key={elementKey}
                        typeName={typeName}
                        title={configKey}
                        field={configField}
                        value={configValue}
                        onChange={onChange}
                        autoFocus={autoFocus} />
        );
      case 'dropdown':
        return (
          <DropdownField key={elementKey}
                         typeName={typeName}
                         title={configKey}
                         field={configField}
                         value={configValue}
                         onChange={onChange}
                         autoFocus={autoFocus}
                         addPlaceholder />
        );
      case 'list':
        return (
          <ListField key={elementKey}
                     typeName={typeName}
                     title={configKey}
                     field={configField}
                     value={configValue}
                     onChange={onChange}
                     autoFocus={autoFocus} />
        );
      default:
        return null;
    }
  }
}
