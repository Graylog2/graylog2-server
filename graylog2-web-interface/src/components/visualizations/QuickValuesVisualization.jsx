import React, {PropTypes} from 'react';
import Immutable from 'immutable';
import {Panel, ListGroup, ListGroupItem} from 'react-bootstrap';
import crossfilter from 'crossfilter';
import dc from 'dc';
import d3 from 'd3';

const D3Utils = require('../../util/D3Utils');
const StringUtils = require('../../util/StringUtils');
import NumberUtils from 'util/NumberUtils';

// Need to import jQuery from node to be able to send events :grumpy:
import jQuery from 'jquery';
require('!script!../../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../../public/javascripts/bootstrap.min.js');

const QuickValuesVisualization = React.createClass({
  propTypes: {
    id: PropTypes.string,
    config: PropTypes.object,
    width: PropTypes.any,
    height: PropTypes.any,
    horizontal: PropTypes.bool,
    displayAnalysisInformation: PropTypes.bool,
    displayAddToSearchButton: PropTypes.bool,
  },
  getInitialState() {
    this.filters = [];
    this.triggerRender = true;
    this.shouldUpdateData = true;
    this.dcGroupName = 'quickvalue-' + this.props.id;
    this.quickValuesData = crossfilter();
    this.dimension = this.quickValuesData.dimension((d) => d.term);
    this.group = this.dimension.group().reduceSum((d) => d.count);

    return {
      total: undefined,
      others: undefined,
      missing: undefined,
      terms: Immutable.List(),
    };
  },
  componentDidMount() {
    this._resizeVisualization(this.props.width, this.props.height, this.props.config.show_data_table);
    this._formatProps(this.props);
    this._renderDataTable();
    this._renderPieChart();
  },
  componentWillReceiveProps(newProps) {
    this._resizeVisualization(newProps.width, newProps.height, newProps.config.show_data_table);
    this._formatProps(newProps);
  },
  NUMBER_OF_TOP_VALUES: 5,
  DEFAULT_PIE_CHART_SIZE: 200,
  MARGIN_TOP: 15,

  _formatProps(newProps) {
    if (newProps.data) {
      const quickValues = newProps.data;

      const total = quickValues.total - quickValues.missing;

      const terms = Immutable.List(Immutable.Map(quickValues.terms).keys());

      const formattedTerms = terms.map((term) => {
        const count = quickValues.terms[term];
        return Immutable.Map({
          term: StringUtils.escapeHTML(term),
          count: count,
          percentage: count / total,
        });
      });

      this.shouldUpdateData = !formattedTerms.equals(this.state.terms);

      this.setState({
        total: quickValues.total,
        others: quickValues.other,
        missing: quickValues.missing,
        terms: formattedTerms,
      }, this.drawData);
    }
  },
  _getAddToSearchButton(term) {
    const addToSearchButton = document.createElement('button');
    addToSearchButton.className = 'btn btn-xs btn-default';
    addToSearchButton.title = 'Add to search query';
    addToSearchButton.setAttribute('data-term', StringUtils.unescapeHTML(term));
    addToSearchButton.innerHTML = "<i class='fa fa-search-plus'></i>";

    return addToSearchButton.outerHTML;
  },
  _getDataTableColumns() {
    const columns = [
      (d) => {
        let colourBadge = '';

        if (typeof this.pieChart !== 'undefined' && this.dataTable.group()(d) !== 'Others') {
          const colour = this.pieChart.colors()(d.term);
          colourBadge = `<span class="datatable-badge" style="background-color: ${colour}"></span>`;
        }

        return colourBadge + ' ' + d.term;
      },
      (d) => {
        return NumberUtils.formatPercentage(d.percentage);
      },
      (d) => NumberUtils.formatNumber(d.count),
    ];

    if (this.props.displayAddToSearchButton) {
      columns.push((d) => this._getAddToSearchButton(d.term));
    }

    return columns;
  },
  _renderDataTable() {
    const tableDomNode = this.refs.table;

    this.dataTable = dc.dataTable(tableDomNode, this.dcGroupName);
    this.dataTable
      .dimension(this.dimension)
      .group((d) => {
        const topValues = this.group.top(this.NUMBER_OF_TOP_VALUES);
        const dInTopValues = topValues.some((value) => d.term.localeCompare(value.key) === 0);
        return dInTopValues ? 'Top values' : 'Others';
      })
      .size(50)
      .columns(this._getDataTableColumns())
      .sortBy((d) => d.count)
      .order(d3.descending)
      .on('renderlet', (table) => {
        table.selectAll('.dc-table-group').classed('info', true);
        table.selectAll('td.dc-table-column button').on('click', () => {
          const $term = $(d3.event.target).closest('button').data('term');
          jQuery(document).trigger('add-search-term.graylog.search', {field: this.props.id, value: $term});
        });
      });

    this.dataTable.render();
  },
  _renderPieChart() {
    const graphDomNode = this.refs.graph;

    this.pieChart = dc.pieChart(graphDomNode, this.dcGroupName);
    this.pieChart
      .dimension(this.dimension)
      .group(this.group)
      .renderLabel(false)
      .renderTitle(false)
      .slicesCap(this.NUMBER_OF_TOP_VALUES)
      .ordering((d) => d.value)
      .colors(D3Utils.glColourPalette());

    this._resizeVisualization(this.props.width, this.props.height, this.props.config.show_data_table);

    D3Utils.tooltipRenderlet(this.pieChart, 'g.pie-slice', this._formatGraphTooltip);

    $(graphDomNode).tooltip({
      'selector': '[rel="tooltip"]',
      'container': 'body',
      'placement': 'auto',
      'delay': {show: 300, hide: 100},
      'html': true,
    });

    this.pieChart.render();
  },
  _formatGraphTooltip(d) {
    const valueText = `${d.data.key}: ${NumberUtils.formatNumber(d.value)}`;

    return `<div class="datapoint-info">${valueText}</div>`;
  },
  _setPieChartSize(newSize) {
    this.pieChart
      .width(newSize)
      .height(newSize)
      .radius(newSize / 2 - 10);

    this.triggerRender = true;
  },
  _resizeVisualization(width, height, showDataTable) {
    let computedSize;

    if (this.props.config.show_pie_chart) {
      if (showDataTable) {
        computedSize = this.DEFAULT_PIE_CHART_SIZE;
      } else {
        computedSize = Math.min(width, height);
        computedSize -= this.MARGIN_TOP;
      }

      if (this.pieChart !== undefined && this.pieChart.width() !== computedSize) {
        this._setPieChartSize(computedSize);
      }
    }
  },
  _clearDataFilters() {
    if (this.pieChart !== undefined) {
      this.filters = this.pieChart.filters();
      this.pieChart.filterAll();
    }
  },
  _restoreDataFilters() {
    if (this.pieChart !== undefined) {
      this.filters.forEach((filter) => this.pieChart.filter(filter));
      this.filters = [];
    }
  },
  drawData() {
    if (this.shouldUpdateData) {
      this._clearDataFilters();
      this.quickValuesData.remove();
      this.quickValuesData.add(this.state.terms.toJS());
      this._restoreDataFilters();
      this.dataTable.redraw();
    }

    if (this.props.config.show_pie_chart) {
      if (this.triggerRender) {
        this.pieChart.render();
        this.triggerRender = false;
      } else {
        this.pieChart.redraw();
      }
    }
  },
  _getTotalMessagesWithField() {
    return this.state.total - this.state.missing;
  },
  _getAnalysisInformation() {
    const analysisInformation = [`Found <em>${NumberUtils.formatNumber(this._getTotalMessagesWithField())}</em> messages with this field`];

    if (this.state.missing !== 0) {
      let missingMessage = this.state.others === 0 ? ' and' : '';
      missingMessage += ` <em>${NumberUtils.formatNumber(this.state.missing)}</em> messages without it`;
      analysisInformation.push(missingMessage);
    }
    if (this.state.others !== 0) {
      analysisInformation.push(` and <em>${NumberUtils.formatNumber(this.state.others)}</em> other values`);
    }

    return <span dangerouslySetInnerHTML={{__html: analysisInformation.join(',') + '.'}}/>;
  },
  render() {
    let pieChartClassName;
    const pieChartStyle = {};

    if (this.props.config.show_pie_chart) {
      if (this.props.horizontal) {
        pieChartClassName = 'col-md-4';
        pieChartStyle.textAlign = 'center';
      } else {
        pieChartClassName = 'col-md-12';
      }
    } else {
      pieChartClassName = 'hidden';
    }

    let dataTableClassName;

    if (this.props.config.show_data_table) {
      dataTableClassName = this.props.horizontal ? 'col-md-8' : 'col-md-12';
    } else {
      dataTableClassName = 'hidden';
    }

    let pieChart;
    if (this.props.displayAnalysisInformation) {
      pieChart = (
        <Panel>
          <ListGroup fill>
            <ListGroupItem>
              <div ref="graph" className="quickvalues-graph"/>
            </ListGroupItem>
            <ListGroupItem>
              {this._getAnalysisInformation()}
            </ListGroupItem>
          </ListGroup>
        </Panel>
      );
    } else {
      pieChart = <div ref="graph" className="quickvalues-graph"/>;
    }

    return (
      <div id={'visualization-' + this.props.id} className="quickvalues-visualization"
           style={{height: this.props.height}}>
        <div className="container-fluid">
          <div className="row" style={{marginBottom: 0}}>
            <div className={pieChartClassName} style={pieChartStyle}>
              {pieChart}
            </div>
            <div className={dataTableClassName}>
              <div className="quickvalues-table">
                <table ref="table" className="table table-condensed table-hover">
                  <thead>
                  <tr>
                    <th style={{width: '60%'}}>Value</th>
                    <th>%</th>
                    <th>Count</th>
                    {this.props.displayAddToSearchButton &&
                    <th style={{width: 30}}>&nbsp;</th>
                      }
                  </tr>
                  </thead>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  },
});

export default QuickValuesVisualization;
