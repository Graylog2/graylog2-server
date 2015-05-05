'use strict';

var $ = require('jquery');

var React = require('react');

var SearchStore = require('../../stores/search/SearchStore');

var LegacyFieldGraph = React.createClass({
    getInitialState() {
        return {
            graphId: ""
        };
    },
    componentDidMount() {
        var graphContainer = React.findDOMNode(this.refs.fieldGraphContainer);
        $(document).trigger("create.graylog.fieldgraph", {field: this.props.field, container: graphContainer});
        $(document).on("created.graylog.fieldgraph", (graphId) => {
            this.setState({graphId: graphId});
        });
    },
    _getFirstGraphValue() {
        if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
            return null;
        }

        return this.props.from;
    },
    render() {
        //<AddToDashboardMenu title="Add to dashboard"
        //                    dashboards={this.props.dashboards}
        //                    widgetType={Widget.Type.SEARCH_RESULT_CHART}
        //                    configuration={{resolution: this.props.histogram.interval}}
        //                    pullRight={true}/>
        return (
            <div className="content-col">
                <div className="pull-right">

                </div>
                <h1>{this.props.field} graph</h1>

                <div ref="fieldGraphContainer"
                     className="field-graph-container"
                     data-from={this._getFirstGraphValue()}
                     data-to={this.props.to}>
                    <div className="field-graph-components">
                        <div className="field-graph-y-axis"></div>
                        <div className="field-graph"></div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = LegacyFieldGraph;