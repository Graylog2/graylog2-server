'use strict';

var $ = require('jquery');

var React = require('react');
var Button = require('react-bootstrap').Button;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var DropdownButton = require('react-bootstrap').DropdownButton;

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
        // TODO:
        // * add to dashboard
        // * work with streams

        return (
            <div ref="fieldGraphContainer"
                 className="content-col field-graph-container"
                 data-from={this._getFirstGraphValue()}
                 data-to={this.props.to}>
                <div className="pull-right">
                    <ButtonGroup>
                        <Button bsSize='small' className='pin hide-combined-chart'>Pin graph</Button>
                        <Button bsSize='small' className='unpin hide-combined-chart'>Unpin graph</Button>
                        <DropdownButton bsSize='small' className='graph-settings' title='Customize' pullRight>
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

                            <li><a href="#" className="hide">Hide</a></li>
                        </DropdownButton>
                    </ButtonGroup>
                    <div style={{display: 'inline', marginLeft: 20}}>
                        <Button bsSize='small' className='reposition-handle' title='Drag and drop to merge the graph into another'>
                            <i className="fa fa-reorder"></i>
                        </Button>
                    </div>
                </div>
                <h1>{this.props.field} graph</h1>

                <ul className="field-graph-query-container">
                    <li>
                        <div className="field-graph-query-color" style={{backgroundColor: "#4DBCE9"}}></div>
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