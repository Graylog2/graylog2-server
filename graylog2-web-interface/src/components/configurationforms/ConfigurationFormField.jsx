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
                     autoFocus={autoFocus}
                     addPlaceholder />
        );
      default:
        return null;
    }
  }
}
