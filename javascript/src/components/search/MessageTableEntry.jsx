/* global momentHelper */
/* jshint -W079 */

'use strict';

var React = require('react');
var moment = require('moment');
var MessageDetail = require('./MessageDetail');

var MessageTableEntry = React.createClass({
    render() {

        var formattedTime = momentHelper.toUserTimeZone(moment(this.props.message.fields['timestamp'])).format();
        return (
            <tbody className="message-group" >
                <tr className="fields-row" onClick={() => this.props.toggleDetail(this.props.message.id)}>
                    <td><strong><time dateTime={this.props.message.fields['timestamp']}>{formattedTime}</time></strong></td>
                    <td className="result-td-36cd38f49b9afa08222c0dc9ebfe35eb">{this.props.message.fields['source']}</td>
                </tr>
                <tr className="message-row" onClick={() => this.props.toggleDetail(this.props.message.id)}>
                    <td colSpan="2">{this.props.message.fields['message']}</td>
                </tr>
                {this.props.expanded &&
                    <tr className="message-detail-row" style={{display: "table-row"}}>
                        <td colSpan={2}>
                            <MessageDetail message={this.props.message}/>
                        </td>
                    </tr>
                }
            </tbody>
        );
    }
});

module.exports = MessageTableEntry;