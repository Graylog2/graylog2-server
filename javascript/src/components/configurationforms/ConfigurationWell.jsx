'use strict';

var React = require('react/addons');
var $ = require('jquery'); // excluded and shimed

var ConfigurationWell = React.createClass({
    getInitialState() {
        return {
            id: this.props.id,
            configuration: this.props.configuration
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    _formatConfiguration(id, config) {
        if (!config) {
            return ("");
        }
        var formattedItems = $.map(config, (value, key) => {
            return (<li key={id + "-" + key}><div className='key'>{key}:</div> <div className='value'>{value}</div></li>);
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
                {this._formatConfiguration(this.state.id, this.state.configuration)}
            </div>
        );
    }
});

module.exports = ConfigurationWell;
