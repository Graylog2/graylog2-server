'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var AgentsStore = require('../../stores/agents/AgentsStore');

var AgentList = React.createClass({
    AGENT_DATA_REFRESH: 5*1000,

    getInitialState() {
        return {
            agents: [],
            filter: "",
            showInactive: false
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
    _getFilteredAgents() {
        var filter = this.state.filter.toLowerCase().trim();
        return this.state.agents.filter((agent) => { return !filter || agent.id.toLowerCase().indexOf(filter) !== -1
            || agent.node_id.toLowerCase().indexOf(filter) !== -1
            || agent.node_details.operating_system.toLowerCase().indexOf(filter) !== -1; })
    },
    render() {
        var agentList;

        if (this.state.agents.length === 0) {
            agentList = <div><div className="alert alert-info">There are no agents.</div></div>;
        } else {
            var agents = this._getFilteredAgents().filter((agent) => {return (this.state.showInactive || agent.active)}).map((agent) => {
                var agentClass = agent.active ? "" : "greyedOut inactive";
                var style = {}; //agent.active ? {} : {display: 'none'};
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
                <div>
                    <div className="row">
                        <div className="col-md-12">
                            <form className="form-inline agents-filter-form">
                                <label htmlFor="agentsfilter">Filter agents:</label>
                                <input type="text" name="filter" id="agentsfilter" value={this.state.filter} onChange={(event) => {this.setState({filter: event.target.value});}} />
                            </form>

                            <a onClick={this.toggleShowInactive}>Toggle</a> displaying inactive agents

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
                        </div>
                    </div>
                </div>
            );
        }

        return agentList;
    },
    toggleShowInactive() {
        this.setState({showInactive: !this.state.showInactive});
    }
});

module.exports = AgentList;
