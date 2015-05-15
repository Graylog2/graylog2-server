/* global momentHelper */
/* jshint -W079 */

'use strict';

var React = require('react/addons');
var moment = require('moment');

var CollectorRow = React.createClass({
    getInitialState() {
        return {
            showRelativeTime: true
        };
    },
    _getOsGlyph(operatingSystem) {
        var glyphClass = "fa-question-circle";
        var os = operatingSystem.trim().toLowerCase();
        if (os.indexOf("mac os") > -1) {
            glyphClass = "fa-apple";
        }
        if (os.indexOf("linux") > -1) {
            glyphClass = "fa-linux";
        }
        if (os.indexOf("win") > -1) {
            glyphClass = "fa-windows";
        }

        return (<i className={"fa " + glyphClass}></i>);
    },
    _toggleRelativeTime() {
        this.setState({showRelativeTime: !this.state.showRelativeTime});
    },
    render() {
        var collector = this.props.collector;
        var collectorClass = collector.active ? "" : "greyed-out inactive";
        var style = {};
        var annotation = collector.active ? "" : "(inactive)";
        var osGlyph = this._getOsGlyph(collector.node_details.operating_system);
        var formattedTime = (this.state.showRelativeTime ? moment(collector.last_seen).fromNow() :  momentHelper.toUserTimeZone(moment(collector.last_seen)).format());
        return (
            <tr className={collectorClass} style={style}>
                <td className="limited">
                    {collector.id}
                    {annotation}
                </td>
                <td className="limited">
                    {collector.node_id}
                </td>
                <td className="limited">
                    {collector.collector_version}
                </td>
                <td className="limited">
                    {osGlyph}
                    {collector.node_details.operating_system}
                </td>
                <td className="limited">
                    <time dateTime={collector.last_seen} onClick={this._toggleRelativeTime}>{formattedTime}</time>
                </td>
            </tr>
        );
    }
});

module.exports = CollectorRow;
