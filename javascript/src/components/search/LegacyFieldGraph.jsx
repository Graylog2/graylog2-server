/* global getReadableFieldChartStatisticalFunction */

'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var Button = require('react-bootstrap').Button;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;

var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');
var Widget = require('../widgets/Widget');

var SearchStore = require('../../stores/search/SearchStore');
var FieldGraphsStore = require('../../stores/field-analyzers/FieldGraphsStore');

var PureRenderMixin = require('react-addons-pure-render-mixin');
var StringUtils = require('../../util/StringUtils');

var LegacyFieldGraph = React.createClass({
    mixins: [PureRenderMixin],
    statisticalFunctions: ['mean', 'max', 'min', 'total', 'count', 'cardinality'],
    renderers: ['area', 'bar', 'line', 'scatterplot'],
    interpolations: ['linear', 'step-after', 'basis', 'bundle', 'cardinal', 'monotone'],
    resolutions: ['minute', 'hour', 'day', 'week', 'month', 'quarter', 'year'],
    componentDidMount() {
        var graphContainer = ReactDOM.findDOMNode(this.refs.fieldGraphContainer);
        FieldGraphsStore.renderFieldGraph(this.props.graphOptions, graphContainer);
    },
    _getFirstGraphValue() {
        if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
            return null;
        }

        return this.props.from;
    },
    _getGraphTitle() {
        return this.props.stacked ? "Combined graph" : this.props.graphOptions['field'] + " graph";
    },
    _getWidgetType() {
        return this.props.stacked ? Widget.Type.STACKED_CHART : Widget.Type.FIELD_CHART;
    },
    _getWidgetConfiguration() {
        return this.props.stacked ? FieldGraphsStore.getStackedGraphAsCreateWidgetRequestParams(this.props.graphId) :
            FieldGraphsStore.getFieldGraphAsCreateWidgetRequestParams(this.props.graphId);
    },
    _submenuItemClassName(configKey, value) {
        return this.props.graphOptions[configKey] === value ? 'selected' : '';
    },
    _getSubmenu(configKey, values) {
        const submenuItems = values.map(value => {
            const readableName = (configKey === 'valuetype') ? getReadableFieldChartStatisticalFunction(value) : value;
            return (
                <li key={`menu-item-${value}`}>
                  <a href="#" className={this._submenuItemClassName(configKey, value)} data-type={value}>
                      {StringUtils.capitalizeFirstLetter(readableName)}
                  </a>
                </li>
            );
        });

        return <ul className={`dropdown-menu ${configKey}-selector`}>{submenuItems}</ul>;
    },
    render() {
        let submenus = [
            <li key="renderer-submenu" className="dropdown-submenu left-submenu">
                <a href="#">Type</a>
                {this._getSubmenu('renderer', this.renderers)}
            </li>,
            <li key="interpolation-submenu" className="dropdown-submenu left-submenu">
                <a href="#">Interpolation</a>
                {this._getSubmenu('interpolation', this.interpolations)}
            </li>
        ];

        if (!this.props.stacked) {
            submenus.unshift(
              <li key="valuetype-submenu" className="dropdown-submenu left-submenu">
                  <a href="#">Value</a>
                  {this._getSubmenu('valuetype', this.statisticalFunctions)}
              </li>
            );
            submenus.push(
              <li key="resolution-submenu" className="dropdown-submenu left-submenu">
                  <a href="#">Resolution</a>
                  {this._getSubmenu('interval', this.resolutions)}
              </li>
            );
        }

        return (
            <div ref="fieldGraphContainer"
                 style={{display: this.props.hidden ? "none" : "block"}}
                 className="content-col field-graph-container"
                 data-chart-id={this.props.graphId}
                 data-from={this._getFirstGraphValue()}
                 data-to={this.props.to}
                 data-field={this.props.graphOptions.field}>
                <div className="pull-right">
                    <AddToDashboardMenu title='Add to dashboard'
                                        dashboards={this.props.dashboards}
                                        widgetType={this._getWidgetType()}
                                        configuration={this._getWidgetConfiguration()}
                                        bsStyle='default'
                                        pullRight={true}
                                        permissions={this.props.permissions}>
                        <DropdownButton bsSize='small' className='graph-settings' title='Customize' id="customize-field-graph-dropdown">
                            {submenus}
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
                <h1>{this._getGraphTitle()}</h1>

                <ul className="field-graph-query-container">
                    <li>
                        <div className="field-graph-query-color" style={{backgroundColor: "#4DBCE9"}}></div>
                        &nbsp;
                        <span className="type-description"></span>
                        Query: <span className="field-graph-query"></span>
                    </li>
                </ul>

                <div className="field-graph-components">
                    <div className="field-graph-y-axis" style={{display: 'none'}}></div>
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