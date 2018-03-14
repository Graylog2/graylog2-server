import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Immutable from 'immutable';
import { ListGroup, ListGroupItem, Panel } from 'react-bootstrap';
import crossfilter from 'crossfilter';
import dc from 'dc';
import d3 from 'd3';
import deepEqual from 'deep-equal';
import $ from 'jquery';
import DateTime from 'logic/datetimes/DateTime';

import D3Utils from 'util/D3Utils';
import StringUtils from 'util/StringUtils';
import NumberUtils from 'util/NumberUtils';

import StoreProvider from 'injection/StoreProvider';

import style from './QuickValuesVisualization.css';

global.jQuery = $;
require('bootstrap/js/tooltip');

const SearchStore = StoreProvider.getStore('Search');

const QuickValuesVisualization = createReactClass({
  displayName: 'QuickValuesVisualization',

  DEFAULT_CONFIG: {
    show_pie_chart: true,
    show_data_table: true,
    data_table_limit: 50,
    limit: 5,
    sort_order: 'desc',
  },

  propTypes: {
    id: PropTypes.string.isRequired,
    field: PropTypes.string.isRequired,
    fields: PropTypes.arrayOf(PropTypes.string).isRequired,
    config: PropTypes.shape({
      show_pie_chart: PropTypes.bool,
      show_data_table: PropTypes.bool,
      data_table_limit: PropTypes.number,
      limit: PropTypes.number,
      sort_order: PropTypes.oneOf(['asc', 'desc']),
    }),
    width: PropTypes.any,
    height: PropTypes.any,
    horizontal: PropTypes.bool,
    displayAnalysisInformation: PropTypes.bool,
    displayAddToSearchButton: PropTypes.bool,
    interactive: PropTypes.bool,
    onRenderComplete: PropTypes.func,
    limitHeight: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      config: this.DEFAULT_CONFIG,
      fields: [],
      width: undefined,
      height: undefined,
      horizontal: false,
      displayAnalysisInformation: false,
      displayAddToSearchButton: false,
      interactive: true,
      limitHeight: true,
      onRenderComplete: () => {},
    };
  },

  getInitialState() {
    this.filters = [];
    this.triggerRender = true;
    this.shouldUpdateData = true;
    this.dcGroupName = `quickvalue-${this.props.id}`;
    this.quickValuesData = crossfilter();
    this.dimensionByTerm = this.quickValuesData.dimension(d => d.term);
    this.dimensionByCount = this.quickValuesData.dimension(d => d.count);
    this.group = this.dimensionByTerm.group().reduceSum(d => d.count);

    return {
      total: undefined,
      others: undefined,
      missing: undefined,
      terms: Immutable.List(),
      termsMapping: {},
    };
  },

  componentDidMount() {
    this.disableTransitions = dc.disableTransitions;
    dc.disableTransitions = !this.props.interactive;
    this._resizeVisualization(this.props.width, this.props.height, this._getConfig('show_data_table'));
    this._formatProps(this.props);
    this._renderDataTable(this.props);
    this._renderPieChart(this.props);
  },

  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }

    if (!deepEqual(this.props.config, nextProps.config)) {
      this._renderDataTable(nextProps);
      this._renderPieChart(nextProps);
      return;
    }

    this._resizeVisualization(nextProps.width, nextProps.height, this._getConfig('show_data_table', nextProps.config));
    this._formatProps(nextProps);
  },

  componentWillUnmount() {
    dc.disableTransitions = this.disableTransitions;
  },

  _graph: undefined,
  _table: undefined,
  DEFAULT_PIE_CHART_SIZE: 200,
  MARGIN_TOP: 15,
  _pieChartRendered: false,
  _dataTableRendered: false,

  _handleRender(viz) {
    return () => {
      if (this._dataTableRendered && this._pieChartRendered) {
        return;
      }
      this._dataTableRendered = this._dataTableRendered || viz === 'dataTable';
      this._pieChartRendered = this._pieChartRendered || viz === 'pieChart';

      if (this._dataTableRendered && this._pieChartRendered) {
        this.props.onRenderComplete();
      }
    };
  },

  _getConfig(key, newConfig) {
    const config = newConfig || {};
    const propsConfig = this.props.config || {};
    const defaultConfig = this.DEFAULT_CONFIG;

    if (Object.prototype.hasOwnProperty.call(config, key)) {
      return config[key];
    } else if (Object.prototype.hasOwnProperty.call(propsConfig, key)) {
      return propsConfig[key];
    } else if (Object.prototype.hasOwnProperty.call(defaultConfig, key)) {
      return defaultConfig[key];
    } else {
      throw new Error(`Couldn't find config key "${key}" in any data source`);
    }
  },

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
        termsMapping: quickValues.terms_mapping,
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

  _getFieldName(i) {
    if (this.props.fields.length === 0) {
      // calculate the fields from the data props
      const anyKey = Object.keys(this.props.data.terms_mapping)[0];
      const fields = this.props.data.terms_mapping[anyKey].map(a => a.field);
      return fields[i];
    } else {
      return this.props.fields[i];
    }
  },

  _getDataTableColumns() {
    function formatTimestamp(timestamp) {
      return new DateTime(Number(timestamp)).toString(DateTime.Formats.TIMESTAMP);
    }

    const columns = [
      (d) => {
        let colourBadge = '';

        let formattedTerm = d.term;
        // if we have terms_mapping, this is a stacked quick values, check every term for its field type
        if (this.props.data.terms_mapping && this.props.data.terms_mapping[d.term]) {
          // Separate the terms with a character that is unusual in terms and use a different text color to make the
          // different terms in the stacked value more visible.
          formattedTerm = this.props.data.terms_mapping[d.term]
            .map((t, i) => (this._getFieldName(i) === 'timestamp' ? formatTimestamp(t.value) : t.value))
            .join(' <strong style="color: #999999;">&mdash;</strong> ');
        } else if (this.props.field === 'timestamp') { // only a single field, just check for the timestamp
          // convert unix timestamp to proper formatted value, so that add to search button works correctly
          formattedTerm = formatTimestamp(d.term);
        }
        if (typeof this.pieChart !== 'undefined' && this.dataTable.group()(d) !== 'Others') {
          const colour = this.pieChart.colors()(d.term);
          colourBadge = `<span class="datatable-badge" style="background-color: ${colour} !important;"></span>`;
        }

        return `${colourBadge} ${formattedTerm}`;
      },
      d => NumberUtils.formatPercentage(d.percentage),
      d => NumberUtils.formatNumber(d.count),
    ];

    if (this.props.displayAddToSearchButton) {
      // the timestamp formatting needs to be done in SearchStore.addSearchTerm based on the parameter `field`
      // if we format it here, it won't match the raw data and not all fields will be added if the first stacked
      // field is timestamp. See https://github.com/Graylog2/graylog2-server/issues/4509
      columns.push(d => this._getAddToSearchButton(d.term));
    }

    return columns;
  },

  _getSortOrder(sortOrder) {
    switch (sortOrder) {
      case 'desc': return d3.descending;
      case 'asc': return d3.ascending;
      default: return d3.descending;
    }
  },

  _groupOrderFunc(sortOrder) {
    return (d) => {
      if (sortOrder === 'asc') {
        return d * -1;
      } else {
        return d;
      }
    };
  },

  _renderDataTable(props) {
    const tableDomNode = this._table;
    const limit = this._getConfig('limit', props.config);
    const dataTableLimit = this._getConfig('data_table_limit', props.config);
    const sortOrder = this._getConfig('sort_order', props.config);

    this.dataTable = dc.dataTable(tableDomNode, this.dcGroupName);
    this.dataTable
      .dimension(this.dimensionByCount)
      .group((d) => {
        const topValues = this.group.order(this._groupOrderFunc(sortOrder)).top(limit);
        const dInTopValues = topValues.some(value => d.term.localeCompare(value.key) === 0);
        const dataTableTitle = `${sortOrder === 'desc' ? 'Top' : 'Bottom'} ${limit} values`;
        return dInTopValues ? dataTableTitle : 'Others';
      })
      .sortBy(d => d.count)
      .order(this._getSortOrder(sortOrder))
      .size(dataTableLimit)
      .columns(this._getDataTableColumns());

    if (this.props.interactive) {
      this.dataTable.on('renderlet', (table) => {
        table.selectAll('.dc-table-group').classed('info', true);
        table.selectAll('td.dc-table-column button').on('click', () => {
          const term = $(d3.event.target).closest('button').data('term');
          SearchStore.addSearchTermWithMapping(this.state.termsMapping, props.id, term);
        });
      });
    }

    this.dataTable.on('postRender', this._handleRender('dataTable'));

    this.dataTable.render();
  },

  _renderPieChart(props) {
    const graphDomNode = this._graph;

    this.pieChart = dc.pieChart(graphDomNode, this.dcGroupName);
    this.pieChart
      .dimension(this.dimensionByTerm)
      .group(this.group)
      .othersGrouper((topRows) => {
        const chart = this.pieChart;
        const allRows = chart.group().all();
        const allKeys = allRows.map(chart.keyAccessor());
        const topKeys = topRows.map(chart.keyAccessor());
        const topSet = d3.set(topKeys);
        const topRowsSum = d3.sum(topRows, dc.pluck('value'));
        const otherCount = this.state.total - this.state.missing - topRowsSum;

        return topRows.concat([{ others: allKeys.filter(d => !topSet.has(d)), key: 'Others', value: otherCount }]);
      })
      .renderLabel(false)
      .renderTitle(false)
      .slicesCap(this._getConfig('limit', props.config))
      .ordering(d => d.value)
      .colors(D3Utils.glColourPalette());

    this.pieChart.on('postRender', this._handleRender('pieChart'));
    this._resizeVisualization(props.width, props.height, this._getConfig('show_data_table', props.config));

    if (this.props.interactive) {
      D3Utils.tooltipRenderlet(this.pieChart, 'g.pie-slice', this._formatGraphTooltip);

      $(graphDomNode).tooltip({
        selector: '[rel="tooltip"]',
        container: 'body',
        placement: 'auto',
        delay: { show: 300, hide: 100 },
        html: true,
      });
    }

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
      .radius((newSize / 2) - 10);

    this.triggerRender = true;
  },

  _resizeVisualization(width, height, showDataTable) {
    let computedSize;

    if (this._getConfig('show_pie_chart')) {
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
      this.filters.forEach(filter => this.pieChart.filter(filter));
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

    if (this._getConfig('show_pie_chart')) {
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
    const analysisInformation = [`Found <em>${NumberUtils.formatNumber(this._getTotalMessagesWithField())}</em> messages with field <em>${this.props.field}</em>`];

    if (this.state.missing !== 0) {
      let missingMessage = this.state.others === 0 ? ' and' : '';
      missingMessage += ` <em>${NumberUtils.formatNumber(this.state.missing)}</em> messages without field <em>${this.props.field}</em>`;
      analysisInformation.push(missingMessage);
    }
    if (this.state.others !== 0) {
      analysisInformation.push(` and <em>${NumberUtils.formatNumber(this.state.others)}</em> other values`);
    }

    return <span dangerouslySetInnerHTML={{ __html: `${analysisInformation.join(',')}.` }} />;
  },

  render() {
    const { horizontal, displayAnalysisInformation, height, id, displayAddToSearchButton, limitHeight } = this.props;
    let pieChartClassName;
    const pieChartStyle = {};

    if (this._getConfig('show_pie_chart')) {
      if (horizontal) {
        pieChartClassName = 'col-md-4';
        pieChartStyle.textAlign = 'center';
      } else {
        pieChartClassName = 'col-md-12';
      }
    } else {
      pieChartClassName = 'hidden';
    }

    let dataTableClassName;

    /*
     * Ensure we always render the data table when quickvalues config was created before introducing pie charts,
     * or when neither the data table or the pie chart are selected for rendering.
     */
    if (this._getConfig('show_data_table') || !this._getConfig('show_pie_chart')) {
      dataTableClassName = horizontal ? 'col-md-8' : 'col-md-12';
    } else {
      dataTableClassName = 'hidden';
    }

    let pieChart;
    if (displayAnalysisInformation) {
      pieChart = (
        <Panel>
          <ListGroup fill>
            <ListGroupItem>
              <div ref={(c) => { this._graph = c; }} className="quickvalues-graph" />
            </ListGroupItem>
            <ListGroupItem>
              {this._getAnalysisInformation()}
            </ListGroupItem>
          </ListGroup>
        </Panel>
      );
    } else {
      pieChart = <div ref={(c) => { this._graph = c; }} className="quickvalues-graph" />;
    }

    return (
      <div id={`visualization-${id}`}
           className="quickvalues-visualization"
           style={limitHeight ? { height: height } : {}}>
        <div className="container-fluid">
          <div className="row" style={{ marginBottom: 0 }}>
            <div className={`${pieChartClassName} ${style.pieChart}`} style={pieChartStyle}>
              {pieChart}
            </div>
            <div className={dataTableClassName}>
              <div className="quickvalues-table">
                <table ref={(c) => { this._table = c; }} className="table table-condensed table-hover">
                  <thead>
                    <tr>
                      <th style={{ width: '60%' }}>Value</th>
                      <th>%</th>
                      <th>Count</th>
                      {displayAddToSearchButton &&
                      <th style={{ width: 30 }}>&nbsp;</th>
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
