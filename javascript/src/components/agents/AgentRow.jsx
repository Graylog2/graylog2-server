'use strict';

var React = require('react/addons');

var AgentRow = React.createClass({
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
    render() {
        var agent = this.props.agent;
        var agentClass = agent.active ? "" : "greyed-out inactive";
        var style = {};
        var annotation = agent.active ? "" : "(inactive)";
        var osGlyph = this._getOsGlyph(agent.node_details.operating_system);
        return (
            <tr className={agentClass} style={style}>
                <td className="limited">
                    {agent.id}
                    {annotation}
                </td>
                <td className="limited">
                    {agent.node_id}
                </td>
                <td className="limited">
                    {agent.agent_version}
                </td>
                <td className="limited">
                    {osGlyph}
                    {agent.node_details.operating_system}
                </td>
                <td className="limited">
                    {moment(agent.last_seen).fromNow()}
                </td>
            </tr>
        );
    }
});

module.exports = AgentRow;
