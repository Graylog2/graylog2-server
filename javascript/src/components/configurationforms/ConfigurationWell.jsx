'use strict';

var React = require('react/addons');

var ConfigurationWell = React.createClass({
    getInitialState() {
        return {
            id: this.props.id,
            configuration: this.props.configuration
        };
    },
    _formatConfiguration(id, config) {
        var formattedItems = $.map(config, (value, key) => {
            return (<li key={id + "-" + key}>{key}: {value}</li>);
        });

        if (formattedItems.length < 1)
            formattedItems.push(<li key="placeholder">-- no configuration --</li>);

        return (
            <ul>
                {formattedItems}
            </ul>
        );
    },
    render() {
        return (
            <div className="well well-small">
                {this._formatConfiguration(this.state.id, this.state.configuration)}
            </div>
        );
    }
});

module.exports = ConfigurationWell;
