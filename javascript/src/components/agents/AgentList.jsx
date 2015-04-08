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
            sort: undefined,
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
    _bySortField(agent1, agent2) {
        var sort = this.state.sort || ((agent) => {return agent.id});
        var field1 = sort(agent1);
        var field2 = sort(agent2);
        if (typeof(field1) === "number") {
            return field2 - field1;
        } else {
            return field1.localeCompare(field2);
        }
    },
    render() {
        var agentList;

        if (this.state.agents.length === 0) {
            agentList = <div><div className="alert alert-info">There are no agents.</div></div>;
        } else {
            var agents = this._getFilteredAgents()
                .filter((agent) => {return (this.state.showInactive || agent.active)})
                .sort(this._bySortField)
                .map((agent) => {
                    var agentClass = agent.active ? "" : "greyedOut inactive";
                    var style = {}; //agent.active ? {} : {display: 'none'};
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
                                {osGlyph}
                                {agent.node_details.operating_system}
                            </td>
                            <td className="limited">
                                {moment(agent.last_seen).fromNow()}
                            </td>
                            <td className="limited">
                            </td>
                        </tr>
                    );
                }
            );

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
                                    <th className="name" onClick={this.sortById}>
                                        Agent ID
                                    </th>
                                    <th onClick={this.sortByNodeId}>Node Id</th>
                                    <th onClick={this.sortByOperatingSystem}>Operating System</th>
                                    <th onClick={this.sortByLastSeen}>Last Seen</th>
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
    },
    sortById() {
        this.setState({sort: (agent) => {return agent.id}});
    },
    sortByNodeId() {
        this.setState({sort: (agent) => {return agent.node_id}});
    },
    sortByOperatingSystem() {
        this.setState({sort: (agent) => {return agent.node_details.operating_system}});
    },
    sortByLastSeen() {
        this.setState({sort: (agent) => {return agent.last_seen}});
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
    }
});

module.exports = AgentList;
