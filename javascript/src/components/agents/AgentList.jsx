'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var AgentsStore = require('../../stores/agents/AgentsStore');

var AgentList = React.createClass({
    AGENT_DATA_REFRESH: 5*1000,

    getInitialState() {
        return {
            agents: []
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        AgentsStore.load((agents) => {
            if (this.isMounted()) {
                this.setState({
                    agents: agents
                });
            }
        });

        setTimeout(this.loadData, this.AGENT_DATA_REFRESH);
    },
    render() {
        var agentList;

        if (this.state.agents.length === 0) {
            agentList = <div><div className="alert alert-info">There are no agents.</div></div>;
        } else {
            var agents = this.state.agents.map((agent) => {
                var agentClass = agent.active ? "" : "greyedOut inactive";
                var style = agent.active ? {} : {display: 'none'};
                var annotation = agent.active ? "" : "(inactive)";
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
                            {agent.node_details.operating_system}
                        </td>
                        <td className="limited">
                            {agent.last_seen}
                        </td>
                        <td className="limited">
                        </td>
                    </tr>
                );
            });

            agentList = (
                <table className="table table-striped users-list">
                    <thead>
                    <tr>
                        <th className="name">
                            Agent ID
                        </th>
                        <th>Node Id</th>
                        <th>Operating System</th>
                        <th>Last Seen</th>
                        <th className="actions">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {agents}
                    </tbody>
                </table>
            );
        }

        return agentList;
    }
});

module.exports = AgentList;
