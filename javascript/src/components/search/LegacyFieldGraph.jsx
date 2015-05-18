'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;

var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');
var Widget = require('../widgets/Widget');

var SearchStore = require('../../stores/search/SearchStore');
var FieldGraphsStore = require('../../stores/field-analyzers/FieldGraphsStore');

var LegacyFieldGraph = React.createClass({
    componentDidMount() {
        var graphContainer = React.findDOMNode(this.refs.fieldGraphContainer);
        FieldGraphsStore.renderFieldGraph(this.props.graphOptions, graphContainer);
    },
    _getFirstGraphValue() {
        if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
            return null;
        }

        return this.props.from;
    },
    render() {
        // TODO: work with streams

        return (
            <div ref="fieldGraphContainer"
                 className="content-col field-graph-container"
                 data-chart-id={this.props.graphId}
                 data-from={this._getFirstGraphValue()}
                 data-to={this.props.to}>
                <div className="pull-right">
                    <AddToDashboardMenu title='Add to dashboard'
                                        dashboards={this.props.dashboards}
                                        widgetType={Widget.Type.FIELD_CHART}
                                        configuration={FieldGraphsStore.getGraphOptionsAsCreateWidgetRequestParams(this.props.graphId)}
                                        bsStyle='default'
                                        pullRight={true}
                                        permissions={this.props.permissions}>
                        <DropdownButton bsSize='small' className='graph-settings' title='Customize'>
                            <li className="dropdown-submenu left-submenu hide-combined-chart">
                                <a href="#">Value</a>

                                <ul className="dropdown-menu valuetype-selector">
                                    <li><a href="#" className="selected" data-type="mean">mean</a></li>
                                    <li><a href="#" data-type="max">maximum</a></li>
                                    <li><a href="#" data-type="min">minimum</a></li>
                                    <li><a href="#" data-type="total">total</a></li>
                                    <li><a href="#" data-type="count">count</a></li>
                                </ul>
                            </li>

                            <li className="dropdown-submenu left-submenu">
                                <a href="#">Type</a>

                                <ul className="dropdown-menu type-selector">
                                    <li><a href="#" data-type="area">Area</a></li>
                                    <li><a href="#" className="selected" data-type="bar">Bar</a></li>
                                    <li><a href="#" data-type="line">Line</a></li>
                                    <li><a href="#" data-type="scatterplot">Scatterplot</a></li>
                                </ul>
                            </li>

                            <li className="dropdown-submenu left-submenu">
                                <a href="#">Interpolation</a>

                                <ul className="dropdown-menu interpolation-selector">
                                    <li><a href="#" className="selected" data-type="linear">linear</a></li>
                                    <li><a href="#" data-type="step-after">step-after</a></li>
                                    <li><a href="#" data-type="basis">basis</a></li>
                                    <li><a href="#" data-type="bundle">bundle</a></li>
                                    <li><a href="#" data-type="cardinal">cardinal</a></li>
                                    <li><a href="#" data-type="monotone">monotone</a></li>
                                </ul>
                            </li>

                            <li className="dropdown-submenu left-submenu hide-combined-chart">
                                <a href="#">Resolution</a>

                                <ul className="dropdown-menu interval-selector">
                                    <li><a href="#" data-type="minute">Minute</a></li>
                                    <li><a href="#" data-type="hour">Hour</a></li>
                                    <li><a href="#" data-type="day">Day</a></li>
                                    <li><a href="#" data-type="week">Week</a></li>
                                    <li><a href="#" data-type="month">Month</a></li>
                                    <li><a href="#" data-type="quarter">Quarter</a></li>
                                    <li><a href="#" data-type="year">Year</a></li>
                                </ul>
                            </li>

                            <MenuItem divider={true}/>
                            <MenuItem onSelect={this.props.onDelete}>Dismiss</MenuItem>
                        </DropdownButton>
                    </AddToDashboardMenu>

                    <div style={{display: 'inline', marginLeft: 20}}>
                        <Button href='#'
                                bsSize='small'
                                className='reposition-handle'
                                onClick={(e) => e.preventDefault()}
                                title='Drag and drop to merge the graph into another'>
                            <i className="fa fa-reorder"></i>
                        </Button>
                    </div>
                </div>
                <h1>{this.props.graphOptions['field']} graph</h1>

                <ul className="field-graph-query-container">
                    <li>
                        <div className="field-graph-query-color" style={{backgroundColor: "#4DBCE9"}}></div>
                        &nbsp;
                        <span className="type-description"></span>
                        Query: <span className="field-graph-query"></span>
                    </li>
                </ul>

                <div className="field-graph-components">
                    <div className="field-graph-y-axis"></div>
                    <div className="field-graph"></div>
                </div>

                <div className="merge-hint">
                    <span className="alpha70">Drop to merge charts</span>
                </div>
            </div>
        );
    }
});

module.exports = LegacyFieldGraph;