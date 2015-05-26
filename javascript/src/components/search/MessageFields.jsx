'use strict';

var React = require('react');
var Immutable = require('immutable');
var MessageFieldDescription = require('./MessageFieldDescription');
var PureRenderMixin = require('react/addons').addons.PureRenderMixin;

var MessageFields = React.createClass({
    mixins: [PureRenderMixin],
    render() {
        var fields = [];
        var formattedFields = Immutable.Map(this.props.message['formatted_fields']).sortBy((value, key) => key, (a, b) => a.localeCompare(b));
        formattedFields.forEach((value, key) => {
            fields.push(<dt key={key + "Title"}>{key}</dt>);
            fields.push(<MessageFieldDescription key={key + "Description"}
                                                 message={this.props.message}
                                                 fieldName={key}
                                                 fieldValue={value}
                                                 possiblyHighlight={this.props.possiblyHighlight}
                                                 disableFieldActions={this.props.disableFieldActions}/>);
        });

        return (
            <dl className="message-details message-details-fields">
                {fields}
            </dl>
        );
    }
});

module.exports = MessageFields;