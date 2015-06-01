'use strict';

var React = require('react/addons');
var CollectorsStore = require('../../stores/collectors/CollectorsStore');
var CollectorRow = require('./CollectorRow');
var Spinner = require('../common/Spinner');
var Col = require('react-bootstrap').Col;
var Row = require('react-bootstrap').Row;
var Alert = require('react-bootstrap').Alert;

var CollectorList = React.createClass({
    COLLECTOR_DATA_REFRESH: 5*1000,

    getInitialState() {
        return {
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
    _formatCollectorList(collectors) {
        return (
            <table className="table table-striped collectors-list">
                <thead>
                <tr>
                    <th onClick={this.sortByNodeId}>Host Name</th>
                    <th onClick={this.sortByOperatingSystem}>Operating System</th>
                    <th onClick={this.sortByLastSeen}>Last Seen</th>
                    <th className="name" onClick={this.sortById}>
                        Collector Id
                    </th>
                    <th onClick={this.sortByCollectorVersion}>Collector Version</th>
                    <th>&nbsp;</th>
                </tr>
                </thead>
                <tbody>
                {collectors}
                </tbody>
            </table>
        );
    },
    _formatEmptyListAlert() {
        var showInactiveHint = (this.state.showInactive ? null : " and/or click on \"Include inactive collectors\"");
        return <Alert>There are no collectors to show. Try adjusting your search filter{showInactiveHint}.</Alert>;
    },
    render() {
        if (this.state.collectors) {
            var collectors = this._getFilteredCollectors()
                .filter((collector) => {return (this.state.showInactive || collector.active);})
                .sort(this._bySortField)
                .map((collector) => {
                    return <CollectorRow key={collector.id} collector={collector}/>;
                }
            );

            var showOrHideInactive = (this.state.showInactive ? "Hide" : "Include");

            var collectorList = (collectors.length > 0 ? this._formatCollectorList(collectors) : this._formatEmptyListAlert());

            return (
                <Row>
                    <Col md={12}>
                        <a onClick={this.toggleShowInactive} className="btn btn-primary pull-right">{showOrHideInactive} inactive collectors</a>

                        <form className="form-inline collectors-filter-form" onSubmit={(e) => e.preventDefault() }>
                            <div className="form-group form-group-sm">
                                <label htmlFor="collectorsfilter" className="control-label">Filter collectors:</label>
                                <input type="text" name="filter" id="collectorsfilter" className="form-control" value={this.state.filter} onChange={(event) => {this.setState({filter: event.target.value});}} />
                            </div>
                        </form>

                        {collectorList}
                    </Col>
                </Row>
            );
        } else {
            return <Spinner />;
        }
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
