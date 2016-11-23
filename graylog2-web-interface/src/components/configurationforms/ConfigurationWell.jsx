import React from 'react';

const ConfigurationWell = React.createClass({
  propTypes: {
    id: React.PropTypes.string,
    configuration: React.PropTypes.any,
    typeDefinition: React.PropTypes.object,
  },
  PASSWORD_PLACEHOLDER: '********',
  _formatRegularField(value, key) {
    let finalValue;
    if (value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
      finalValue = <i>{'<empty>'}</i>;
    } else {
      finalValue = Array.isArray(value) ? value.join(', ') : String(value);
    }

    return (<li key={`${this.props.id}-${key}`}><div className="key">{key}:</div> <div className="value">{finalValue}</div></li>);
  },
  _formatPasswordField(value, key) {
    return (<li key={`${this.props.id}-${key}`}><div className="key">{key}:</div> <div className="value">{this.PASSWORD_PLACEHOLDER}</div></li>);
  },
  _formatConfiguration(id, config, typeDefinition) {
    if (!config) {
      return ('');
    }
    const formattedItems = Object.keys(config).sort().map((key) => {
      const value = config[key];
      const requestedConfiguration = (typeDefinition && typeDefinition.requested_configuration ? typeDefinition.requested_configuration[key] : undefined);
      if (requestedConfiguration && requestedConfiguration.attributes.indexOf('is_password') > -1) {
        return this._formatPasswordField(value, key);
      }
      return this._formatRegularField(value, key);
    });

    if (formattedItems.length < 1) {
      formattedItems.push(<li key="placeholder">-- no configuration --</li>);
    }

    return (
      <ul>
        {formattedItems}
      </ul>
    );
  },
  render() {
    return (
      <div className="well well-small configuration-well react-configuration-well">
        {this._formatConfiguration(this.props.id, this.props.configuration, this.props.typeDefinition)}
      </div>
    );
  },
});

export default ConfigurationWell;
