/* global momentHelper */
/* jshint -W079 */

'use strict';

var React = require('react');
var moment = require('moment');
var MessageDetail = require('./MessageDetail');

var MessageTableEntry = React.createClass({

    render() {
        var colSpanFixup = this.props.selectedFields.size + 1;
        var formattedTime = momentHelper.toUserTimeZone(moment(this.props.message.fields['timestamp'])).format();

        var classes = "message-group";
        if (this.props.expanded) {
            classes += " message-group-toggled";
        }
        return (
            <tbody className={classes} >
                <tr className="fields-row" onClick={() => this.props.toggleDetail(this.props.message.id)}>
                    <td><strong><time dateTime={this.props.message.fields['timestamp']}>{formattedTime}</time></strong></td>
                    { this.props.selectedFields.map(selectedFieldName => <td key={selectedFieldName}>{this.props.message.fields[selectedFieldName]}</td>) }
                </tr>

                {this.props.showMessageRow &&
                <tr className="message-row" onClick={() => this.props.toggleDetail(this.props.message.id)}>
                    <td colSpan={colSpanFixup}>{this.props.message.fields['message']}</td>
                </tr>
                }
                {this.props.expanded &&
                    <tr className="message-detail-row" style={{display: "table-row"}}>
                        <td colSpan={colSpanFixup}>
                            <MessageDetail message={this.props.message} inputs={this.props.inputs} streams={this.props.streams} nodes={this.props.nodes}/>
                        </td>
                    </tr>
                }
            </tbody>
        );
    }
});

module.exports = MessageTableEntry;