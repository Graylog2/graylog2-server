/* global momentHelper */
/* jshint -W079 */

'use strict';

var React = require('react');
var moment = require('moment');
var MessageDetail = require('./MessageDetail');
var Immutable = require('immutable');

var MessageTableEntry = React.createClass({

    possiblyHighlight(fieldName) {
        var origValue = this.props.message.fields[fieldName];
        if (this.props.highlight && this.props.message.highlight_ranges) {
            if (this.props.message.highlight_ranges.hasOwnProperty(fieldName)) {
                var chunks = [];
                var highlights = Immutable.fromJS(this.props.message.highlight_ranges[fieldName]).sortBy(range => range.get('start'));
                var position = 0;
                var key = 0;
                highlights.forEach((range, idx) => {
                    if (position !== range.get('start')) {
                        chunks.push(<span key={key++}>{origValue.substring(position, range.get('start'))}</span>);
                    }
                    chunks.push(<span key={key++} className="result-highlight-colored">{origValue.substring(range.get('start'), range.get('start') + range.get('length'))}</span>);
                    if ((idx + 1) < highlights.size) {
                        var nextRange = highlights.get(idx+1);
                        chunks.push(<span key={key++}>{origValue.substring(range.get('start') + range.get('length'), nextRange.get('start'))}</span>);
                        position = nextRange.get('start');
                    } else {
                        chunks.push(<span key={key++}>{origValue.substring(range.get('start')+ range.get('length'))}</span>);
                        position = range.get('start') + range.get('length');
                    }
                });
                return <span>{chunks}</span>;
            } else {
                return String(origValue);
            }
        } else {
            return String(origValue);
        }
    },
    render() {
        var colSpanFixup = this.props.selectedFields.size + 1;
        var formattedTime = momentHelper.toUserTimeZone(moment(this.props.message.fields['timestamp'])).format();

        var classes = "message-group";
        if (this.props.expanded) {
            classes += " message-group-toggled";
        }
        return (
            <tbody className={classes}>
            <tr className="fields-row" onClick={() => this.props.toggleDetail(this.props.message.id)}>
                <td><strong>
                    <time dateTime={this.props.message.fields['timestamp']}>{formattedTime}</time>
                </strong></td>
                { this.props.selectedFields.map(selectedFieldName => <td
                    key={selectedFieldName}>{this.possiblyHighlight(selectedFieldName)}</td>) }
            </tr>

            {this.props.showMessageRow &&
            <tr className="message-row" onClick={() => this.props.toggleDetail(this.props.message.id)}>
                <td colSpan={colSpanFixup}>{this.possiblyHighlight('message')}</td>
            </tr>
            }
            {this.props.expanded &&
            <tr className="message-detail-row" style={{display: "table-row"}}>
                <td colSpan={colSpanFixup}>
                    <MessageDetail message={this.props.message} inputs={this.props.inputs} streams={this.props.streams}
                                   allStreams={this.props.allStreams} allStreamsLoaded={this.props.allStreamsLoaded}
                                   nodes={this.props.nodes} possiblyHighlight={this.possiblyHighlight}/>
                </td>
            </tr>
            }
            </tbody>
        );
    }
});

module.exports = MessageTableEntry;