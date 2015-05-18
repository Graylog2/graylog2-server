'use strict';

var React = require('react/addons');
var CollectorsStore = require('../../stores/collectors/CollectorsStore');
var CollectorRow = require('./CollectorRow');

var CollectorList = React.createClass({
    COLLECTOR_DATA_REFRESH: 5*1000,

    getInitialState() {
        return {
            collectors: [],
            filter: "",
            sort: undefined,
            showInactive: false
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        CollectorsStore.load((collectors) => {
            if (this.isMounted()) {
                this.setState({
                    collectors: collectors
                });
            }
        });

        setTimeout(this.loadData, this.COLLECTOR_DATA_REFRESH);
    },
    _getFilteredCollectors() {
        var filter = this.state.filter.toLowerCase().trim();
        return this.state.collectors.filter((collector) => { return !filter || collector.id.toLowerCase().indexOf(filter) !== -1 || collector.node_id.toLowerCase().indexOf(filter) !== -1 || collector.node_details.operating_system.toLowerCase().indexOf(filter) !== -1; });
    },
    _bySortField(collector1, collector2) {
        var sort = this.state.sort || ((collector) => {return collector.id;});
        var field1 = sort(collector1);
        var field2 = sort(collector2);
        if (typeof(field1) === "number") {
            return field2 - field1;
        } else {
            return field1.localeCompare(field2);
        }
    },
    render() {
        var collectorList;

        if (this.state.collectors.length === 0) {
            collectorList = <div><div className="alert alert-info">There are no collectors.</div></div>;
        } else {
            var collectors = this._getFilteredCollectors()
                .filter((collector) => {return (this.state.showInactive || collector.active);})
                .sort(this._bySortField)
                .map((collector) => {
                    return <CollectorRow key={collector.id} collector={collector}/>;
                }
            );

            var showOrHideInactive = (this.state.showInactive ? "Hide" : "Show");

            collectorList = (
                <div>
                    <div className="row">
                        <div className="col-md-12">
                            <form className="form-inline collectors-filter-form">
                                <label htmlFor="collectorsfilter">Filter collectors:</label>
                                <input type="text" name="filter" id="collectorsfilter" value={this.state.filter} onChange={(event) => {this.setState({filter: event.target.value});}} />
                            </form>

                            <a onClick={this.toggleShowInactive}>{showOrHideInactive}</a> inactive collectors

                            <table className="table table-striped users-list">
                                <thead>
                                <tr>
                                    <th className="name" onClick={this.sortById}>
                                        Collector Id
                                    </th>
                                    <th onClick={this.sortByNodeId}>Host Name</th>
                                    <th onClick={this.sortByCollectorVersion}>Collector Version</th>
                                    <th onClick={this.sortByOperatingSystem}>Operating System</th>
                                    <th onClick={this.sortByLastSeen}>Last Seen</th>
                                </tr>
                                </thead>
                                <tbody>
                                    {collectors}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            );
        }

        return collectorList;
    },
    toggleShowInactive() {
        this.setState({showInactive: !this.state.showInactive});
    },
    sortById() {
        this.setState({sort: (collector) => {return collector.id;}});
    },
    sortByNodeId() {
        this.setState({sort: (collector) => {return collector.node_id;}});
    },
    sortByOperatingSystem() {
        this.setState({sort: (collector) => {return collector.node_details.operating_system;}});
    },
    sortByLastSeen() {
        this.setState({sort: (collector) => {return collector.last_seen;}});
    },
    sortByCollectorVersion() {
        this.setState({sort: (collector) => {return collector.collector_version;}});
    }
});

module.exports = CollectorList;
