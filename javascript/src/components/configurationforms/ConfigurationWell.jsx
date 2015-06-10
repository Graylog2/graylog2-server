'use strict';

var React = require('react/addons');
var $ = require('jquery'); // excluded and shimed

var ConfigurationWell = React.createClass({
    PASSWORD_PLACEHOLDER: '********',
    _formatRegularField(value, key) {
        var finalValue = (value === null || value === undefined || value === "" ? <i>{"<empty>"}</i> : String(value));
        return (<li key={this.props.id + "-" + key}><div className='key'>{key}:</div> <div className='value'>{finalValue}</div></li>);
    },
    _formatPasswordField(value, key) {
        return (<li key={this.props.id + "-" + key}><div className='key'>{key}:</div> <div className='value'>{this.PASSWORD_PLACEHOLDER}</div></li>);
    },
    _formatConfiguration(id, config, typeDefinition) {
        if (!config) {
            return ("");
        }
        var formattedItems = $.map(config, (value, key) => {
            var requestedConfiguration = typeDefinition.requested_configuration[key];
            if (requestedConfiguration && requestedConfiguration.attributes.indexOf('is_password') > -1) {
                return this._formatPasswordField(value, key);
            } else {
                return this._formatRegularField(value, key);
            }
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
    }
});

module.exports = ConfigurationWell;
