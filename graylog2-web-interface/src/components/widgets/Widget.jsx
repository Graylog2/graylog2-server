import React, {PropTypes} from 'react';
import ReactDOM from 'react-dom';
import $ from 'jquery';
import Qs from 'qs';

import URLUtils from 'util/URLUtils';

import { WidgetConfigModal, WidgetEditConfigModal, WidgetFooter, WidgetHeader } from 'components/widgets';

import {
  NumericVisualization,
  HistogramVisualization,
  QuickValuesVisualization,
  GraphVisualization,
  StackedGraphVisualization } from 'components/visualizations';

import WidgetsStore from 'stores/widgets/WidgetsStore';
import WidgetsActions from 'actions/widgets/WidgetsActions';

const Widget = React.createClass({
  propTypes: {
    widget: PropTypes.object.isRequired,
    dashboardId: PropTypes.string.isRequired,
    shouldUpdate: PropTypes.bool.isRequired,
    locked: PropTypes.bool.isRequired,
  },
  statics: {
    Type: {
      SEARCH_RESULT_COUNT: 'SEARCH_RESULT_COUNT',
      STREAM_SEARCH_RESULT_COUNT: 'STREAM_SEARCH_RESULT_COUNT',
      STATS_COUNT: 'STATS_COUNT',
      SEARCH_RESULT_CHART: 'SEARCH_RESULT_CHART',
      QUICKVALUES: 'QUICKVALUES',
      FIELD_CHART: 'FIELD_CHART',
      STACKED_CHART: 'STACKED_CHART',
    },
  },
  getInitialState() {
    return {
      result: undefined,
      calculatedAt: undefined,
      error: false,
      errorMessage: undefined,
      height: undefined,
      width: undefined,
    };
  },
  componentDidMount() {
    this._loadValue();
    this.loadValueInterval = setInterval(this._loadValue, Math.min(this.props.widget.cache_time * 1000, this.DEFAULT_WIDGET_VALUE_REFRESH));
    $(document).on('gridster:resizestop', () => this._calculateWidgetSize());
  },
  componentDidUpdate() {
    this._calculateWidgetSize();
  },
  componentWillUnmount() {
    clearInterval(this.loadValueInterval);
    $(document).off('gridster:resizestop', () => this._calculateWidgetSize());
  },

  DEFAULT_WIDGET_VALUE_REFRESH: 10 * 1000,
  WIDGET_HEADER_HEIGHT: 25,
  WIDGET_FOOTER_HEIGHT: 20,
  _isBoundToStream() {
    return ('stream_id' in this.props.widget.config) && (this.props.widget.config.stream_id !== null);
  },
  _getWidgetNode() {
    return ReactDOM.findDOMNode(this.refs.widget);
  },
  _loadValue() {
    if (!this.props.shouldUpdate) {
      return;
    }

    const width = this.refs.widget.clientWidth;

    WidgetsStore.loadValue(this.props.dashboardId, this.props.widget.id, width).then((value) => {
      const newState = {
        result: value.result,
        calculatedAt: value.calculated_at,
        error: false,
        errorMessage: undefined,
      };

      if (value.computation_time_range) {
        newState.computationTimeRange = value.computation_time_range;
      }

      this.setState(newState);
    }, (response) => {
      const error = response.message;
      const newResult = this.state.result === undefined ? 'N/A' : this.state.result;
      this.setState({
        result: newResult,
        error: true,
        errorMessage: 'Error loading widget value: ' + error,
      });
    });
  },
  _calculateWidgetSize() {
    const $widgetNode = $(this._getWidgetNode());
    // .height() give us the height of the whole widget without counting paddings, we need to remove the size
    // of the header and footer from that.
    const availableHeight = $widgetNode.height() - (this.WIDGET_HEADER_HEIGHT + this.WIDGET_FOOTER_HEIGHT);
    const availableWidth = $widgetNode.width();
    if (availableHeight !== this.state.height || availableWidth !== this.state.width) {
      this.setState({height: availableHeight, width: availableWidth});
    }
  },
  _getVisualization() {
    if (this.props.widget.type === '') {
      return null;
    }

    if (this.state.result === undefined) {
      return (
        <div className="loading">
          <i className="fa fa-spin fa-3x fa-refresh spinner"/>
        </div>
      );
    }

    if (this.state.result === 'N/A') {
      return <div className="not-available">{this.state.result}</div>;
    }

    let visualization;

    switch (this.props.widget.type.toUpperCase()) {
      case this.constructor.Type.SEARCH_RESULT_COUNT:
      case this.constructor.Type.STREAM_SEARCH_RESULT_COUNT:
      case this.constructor.Type.STATS_COUNT:
        visualization = <NumericVisualization data={this.state.result} config={this.props.widget.config}/>;
        break;
      case this.constructor.Type.SEARCH_RESULT_CHART:
        visualization = (<HistogramVisualization id={this.props.widget.id}
                                                 data={this.state.result}
                                                 config={this.props.widget.config}
                                                 computationTimeRange={this.state.computationTimeRange}
                                                 height={this.state.height}
                                                 width={this.state.width}/>);
        break;
      case this.constructor.Type.QUICKVALUES:
        visualization = (<QuickValuesVisualization id={this.props.widget.id}
                                                  config={this.props.widget.config}
                                                  data={this.state.result}
                                                  height={this.state.height}
                                                  width={this.state.width}/>);
        break;
      case this.constructor.Type.FIELD_CHART:
        visualization = (<GraphVisualization id={this.props.widget.id}
                                             data={this.state.result}
                                             config={this.props.widget.config}
                                             computationTimeRange={this.state.computationTimeRange}
                                             height={this.state.height}
                                             width={this.state.width}/>);
        break;
      case this.constructor.Type.STACKED_CHART:
        visualization = (<StackedGraphVisualization id={this.props.widget.id}
                                                   data={this.state.result}
                                                   config={this.props.widget.config}
                                                   height={this.state.height}
                                                   width={this.state.width}/>);
        break;
      default:
        throw new Error(`Error: Widget type '${this.props.widget.type}' not supported`);
    }

    return visualization;
  },
  _getUrlPath() {
    if (this._isBoundToStream()) {
      return '/streams/' + this.props.widget.config.stream_id + '/messages';
    }

    return '/search';
  },
  _getUrlQueryString() {
    const config = this.props.widget.config;
    const rangeType = config.timerange.type;

    const query = {
      q: config.query,
      rangetype: rangeType,
      interval: config.interval,
    };
    switch (rangeType) {
      case 'relative':
        query[rangeType] = config.timerange.range;
        break;
      case 'absolute':
        query.from = config.timerange.from;
        query.to = config.timerange.to;
        break;
      case 'keyword':
        query[rangeType] = config.timerange.keyword;
        break;
    }

    return Qs.stringify(query);
  },
  replayUrl() {
    // TODO: replace with react router link
    const path = this._getUrlPath();
    const queryString = this._getUrlQueryString();

    return URLUtils.appPrefixed(path + '?' + queryString);
  },
  _replaySearch(e) {
    URLUtils.openLink(this.replayUrl(), e.metaKey || e.ctrlKey);
  },
  _showConfig() {
    this.refs.configModal.open();
  },
  _showEditConfig() {
    this.refs.editModal.open();
  },
  updateWidget(newWidgetData) {
    newWidgetData.id = this.props.widget.id;

    WidgetsStore.updateWidget(this.props.dashboardId, newWidgetData);
  },
  deleteWidget() {
    if (window.confirm('Do you really want to delete "' + this.props.widget.description + '"?')) {
      this.setState({deleted: true});
      WidgetsActions.removeWidget(this.props.dashboardId, this.props.widget.id);
    }
  },
  render() {
    if (this.state.deleted) {
      return <span></span>;
    }
    const showConfigModal = (
      <WidgetConfigModal ref="configModal"
                         dashboardId={this.props.dashboardId}
                         widget={this.props.widget}
                         boundToStream={this._isBoundToStream()}/>
    );

    const editConfigModal = (
      <WidgetEditConfigModal ref="editModal"
                             widgetTypes={this.constructor.Type}
                             widget={this.props.widget}
                             onUpdate={this.updateWidget}/>
    );

    return (
      <div ref="widget" className="widget" data-widget-id={this.props.widget.id}>
        <WidgetHeader ref="widgetHeader"
                      title={this.props.widget.description}
                      calculatedAt={this.state.calculatedAt}
                      error={this.state.error}
                      errorMessage={this.state.errorMessage}/>

        {this._getVisualization()}

        <WidgetFooter ref="widgetFooter"
                      locked={this.props.locked}
                      onReplaySearch={this._replaySearch}
                      onShowConfig={this._showConfig}
                      onEditConfig={this._showEditConfig}
                      onDelete={this.deleteWidget}/>
        {this.props.locked ? showConfigModal : editConfigModal}
      </div>
    );
  },
});

export default Widget;
