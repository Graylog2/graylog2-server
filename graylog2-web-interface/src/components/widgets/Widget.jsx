import React from 'react';
import ReactDOM from 'react-dom';
import Reflux from 'reflux';
import $ from 'jquery';
import Qs from 'qs';

import URLUtils from 'util/URLUtils';

import UserNotification from 'util/UserNotification';

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
    widget: React.PropTypes.object.isRequired,
    dashboardGrid: React.PropTypes.object,
    dashboardId: React.PropTypes.string.isRequired,
    shouldUpdate: React.PropTypes.bool.isRequired,
    locked: React.PropTypes.bool.isRequired,
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

    if (this.props.dashboardGrid) {
      this.props.dashboardGrid.add_widget()
    }

    $(document).on('gridster:resizestop', () => this._calculateWidgetSize());
  },
  componentDidUpdate() {
    this._calculateWidgetSize();
  },
  componentWillUnmount() {
    clearInterval(this.loadValueInterval);
    $(document).off('gridster:resizestop', () => this._calculateWidgetSize());
  },
  WIDGET_DATA_REFRESH: 30 * 1000,
  DEFAULT_WIDGET_VALUE_REFRESH: 10 * 1000,
  WIDGET_HEADER_HEIGHT: 25,
  WIDGET_FOOTER_HEIGHT: 20,
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
      this.setState({
        result: value.result,
        calculatedAt: value.calculated_at,
        error: false,
        errorMessage: undefined,
      });
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
      return;
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

    var visualization;

    switch (this.props.widget.type.toUpperCase()) {
      case this.constructor.Type.SEARCH_RESULT_COUNT:
      case this.constructor.Type.STREAM_SEARCH_RESULT_COUNT:
      case this.constructor.Type.STATS_COUNT:
        visualization = <NumericVisualization data={this.state.result} config={this.props.widget.config}/>;
        break;
      case this.constructor.Type.SEARCH_RESULT_CHART:
        visualization = <HistogramVisualization id={this.props.widget.id}
                                                data={this.state.result}
                                                interval={this.props.widget.config.interval}
                                                height={this.state.height}
                                                width={this.state.width}/>;
        break;
      case this.constructor.Type.QUICKVALUES:
        visualization = <QuickValuesVisualization id={this.props.widget.id}
                                                  config={this.props.widget.config}
                                                  data={this.state.result}
                                                  height={this.state.height}
                                                  width={this.state.width}/>;
        break;
      case this.constructor.Type.FIELD_CHART:
        visualization = <GraphVisualization id={this.props.widget.id}
                                            data={this.state.result}
                                            config={this.props.widget.config}
                                            height={this.state.height}
                                            width={this.state.width}/>;
        break;
      case this.constructor.Type.STACKED_CHART:
        visualization = <StackedGraphVisualization id={this.props.widget.id}
                                                   data={this.state.result}
                                                   config={this.props.widget.config}
                                                   height={this.state.height}
                                                   width={this.state.width}/>;
        break;
      default:
        throw('Error: Widget type "' + this.props.widget.type + '" not supported');
    }

    return visualization;
  },
  _getUrlPath() {
    if (this._isBoundToStream()) {
      return '/streams/' + this.props.widget.config.stream_id + '/messages';
    } else {
      return '/search';
    }
  },
  _getUrlQueryString() {
    const config = this.props.widget.config;
    const rangeType = config.timerange.type;

    const query = {
      q: config.query,
      rangetype: rangeType,
      interval: config.interval,
    };
    switch(rangeType) {
      case 'relative':
        query[rangeType] = config.timerange.range;
        break;
      case 'absolute':
        query['from'] = config.timerange.from;
        query['to'] = config.timerange.to;
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
  metricsUrl() {
    // TODO: replace with react router link
    const url = '/system/metrics/master';
    const query = {
      // TODO: replace hardcoded metric name
      prefilter: 'org.graylog2.dashboards.widgets.*.' + this.props.widget.id
    };

    return URLUtils.appPrefixed(url + '?' + Qs.stringify(query));
  },
  _replaySearch(e) {
    URLUtils.openLink(this.replayUrl(), e.metaKey || e.ctrlKey);
  },
  _goToWidgetMetrics() {
    URLUtils.openLink(this.metricsUrl(), false);
  },
  _showConfig() {
    this.refs.configModal.open();
  },
  _showEditConfig() {
    this.refs.editModal.open();

    // Ugly workaround to avoid being able to move a widget when the modal is shown :(
    this._disableGridster();
  },
  _disableGridster() {
    this.props.dashboardGrid.disable();
  },
  _enableGridster() {
    this.props.dashboardGrid.enable();
  },
  updateWidget(newWidgetData) {
    newWidgetData.id = this.props.widget.id;

    WidgetsStore.updateWidget(this.props.dashboardId, newWidgetData).then(() => {
      this.setState({
        title: newWidgetData.title,
        cacheTime: newWidgetData.cacheTime,
        config: newWidgetData.config,
      });
    });
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
                         widget={this.props.widget}
                         boundToStream={this._isBoundToStream()}
                         metricsAction={this._goToWidgetMetrics}/>
    );

    const editConfigModal = (
      <WidgetEditConfigModal ref="editModal"
                             widgetTypes={this.constructor.Type}
                             widget={this.props.widget}
                             onUpdate={this.updateWidget}
                             onModalHidden={this._enableGridster}/>
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
