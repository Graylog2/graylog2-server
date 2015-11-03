/* global assertUpdateEnabled, dashboardGrid */
import React from 'react';
import ReactDOM from 'react-dom';
import $ from 'jquery';
import Qs from 'qs';

import URLUtils from 'util/URLUtils';

import UserNotification from 'util/UserNotification';

import WidgetHeader from 'components/widgets/WidgetHeader';
import WidgetFooter from 'components/widgets/WidgetFooter';
import WidgetConfigModal from 'components/widgets/WidgetConfigModal';
import WidgetEditConfigModal from 'components/widgets/WidgetEditConfigModal';

import NumericVisualization from 'components/visualizations/NumericVisualization';
import HistogramVisualization from 'components/visualizations/HistogramVisualization';
import QuickValuesVisualization from 'components/visualizations/QuickValuesVisualization';
import GraphVisualization from 'components/visualizations/GraphVisualization';
import StackedGraphVisualization from 'components/visualizations/StackedGraphVisualization';

import WidgetsStore from 'stores/widgets/WidgetsStore';

require('!script!legacy/updateManager.js');

var Widget = React.createClass({
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
    }
  },

  getInitialState() {
    return {
      deleted: false,
      locked: true,
      type: '',
      title: '',
      cacheTime: 10,
      creatorUserId: '',
      config: {},
      result: undefined,
      calculatedAt: undefined,
      error: false,
      errorMessage: undefined,
      height: undefined,
      width: undefined,
    };
  },
  _isBoundToStream() {
    return ('stream_id' in this.state.config) && (this.state.config.stream_id !== null);
  },
  _getWidgetData() {
    return {
      widgetId: this.props.widgetId,
      dashboardId: this.props.dashboardId,
      title: this.state.title,
      type: this.state.type,
      cacheTime: this.state.cacheTime,
      creatorUserId: this.state.creatorUserId,
      config: this.state.config
    };
  },
  _getWidgetNode() {
    return ReactDOM.findDOMNode(this.refs.widget);
  },
  componentDidUpdate() {
    this._calculateWidgetSize();
  },
  componentDidMount() {
    this._loadData();
    this._loadValue();

    $(this._getWidgetNode()).on('unlocked.graylog.dashboard', this._dashboardUnlocked);
    $(this._getWidgetNode()).on('locked.graylog.dashboard', this._dashboardLocked);
    $(document).on('gridster:resizestop', () => this._calculateWidgetSize());
  },
  componentWillUnmount() {
    $(this._getWidgetNode()).off('unlocked.graylog.dashboard', this._dashboardUnlocked);
    $(this._getWidgetNode()).off('locked.graylog.dashboard', this._dashboardLocked);
    $(document).off('gridster:resizestop', () => this._calculateWidgetSize());
  },
  _loadData() {
    if (!assertUpdateEnabled(this._loadData) || this.state.deleted) {
      return;
    }

    WidgetsStore.loadWidget(this.props.dashboardId, this.props.widgetId).then((widget) => {
      this.setState({
        type: widget.type,
        title: widget.title,
        cacheTime: widget.cacheTime,
        creatorUserId: widget.creatorUserId,
        config: widget.config
      });
    }, (jqXHR, textStatus, errorThrown) => {
      if (jqXHR.status === 404) {
        UserNotification.warning("It looks like widget \"" + this.state.title + "\" does not exist anymore. " +
          "Please refresh the page to remove it from the dashboard.",
          "Could not load widget information");
        this.setState({deleted: true});
      }
    });
    setTimeout(this._loadData, this.WIDGET_DATA_REFRESH);
  },
  _loadValue() {
    if (!assertUpdateEnabled(this._loadValue) || this.state.deleted) {
      return;
    }

    var width = this.refs.widget.clientWidth;

    WidgetsStore.loadValue(this.props.dashboardId, this.props.widgetId, width).then((value) => {
      this.setState({
        result: value.result,
        calculatedAt: value.calculated_at,
        error: false,
        errorMessage: undefined
      });
    }, (jqXHR, textStatus, errorThrown) => {
      var error = jqXHR.responseText === "" || jqXHR.responseText === "\"\"" ? errorThrown : jqXHR.responseText;
      var newResult = this.state.result === undefined ? "N/A" : this.state.result;
      this.setState({
        result: newResult,
        error: true,
        errorMessage: "Error loading widget value: " + error
      });
    });

    setTimeout(this._loadValue, Math.min(this.state.cacheTime * 1000, this.DEFAULT_WIDGET_VALUE_REFRESH));
  },
  _dashboardLocked() {
    this.setState({locked: true});
  },
  _dashboardUnlocked() {
    this.setState({locked: false});
  },
  _calculateWidgetSize() {
    var $widgetNode = $(this._getWidgetNode());
    // .height() give us the height of the whole widget without counting paddings, we need to remove the size
    // of the header and footer from that.
    var availableHeight = $widgetNode.height() - (this.WIDGET_HEADER_HEIGHT + this.WIDGET_FOOTER_HEIGHT);
    var availableWidth = $widgetNode.width();
    if (availableHeight !== this.state.height || availableWidth !== this.state.width) {
      this.setState({height: availableHeight, width: availableWidth});
    }
  },
  _getVisualization() {
    if (this.state.type === "") {
      return;
    }

    if (this.state.result === undefined) {
      return <div className="loading">
        <i className="fa fa-spin fa-3x fa-refresh spinner"></i>
      </div>;
    }

    if (this.state.result === "N/A") {
      return <div className="not-available">{this.state.result}</div>;
    }

    var visualization;

    switch (this.state.type) {
      case this.constructor.Type.SEARCH_RESULT_COUNT:
      case this.constructor.Type.STREAM_SEARCH_RESULT_COUNT:
      case this.constructor.Type.STATS_COUNT:
        visualization = <NumericVisualization data={this.state.result} config={this.state.config}/>;
        break;
      case this.constructor.Type.SEARCH_RESULT_CHART:
        visualization = <HistogramVisualization id={this.props.widgetId}
                                                data={this.state.result}
                                                interval={this.state.config.interval}
                                                height={this.state.height}
                                                width={this.state.width}/>;
        break;
      case this.constructor.Type.QUICKVALUES:
        visualization = <QuickValuesVisualization id={this.props.widgetId}
                                                  config={this.state.config}
                                                  data={this.state.result}
                                                  height={this.state.height}
                                                  width={this.state.width}/>;
        break;
      case this.constructor.Type.FIELD_CHART:
        visualization = <GraphVisualization id={this.props.widgetId}
                                            data={this.state.result}
                                            config={this.state.config}
                                            height={this.state.height}
                                            width={this.state.width}/>;
        break;
      case this.constructor.Type.STACKED_CHART:
        visualization = <StackedGraphVisualization id={this.props.widgetId}
                                                   data={this.state.result}
                                                   config={this.state.config}
                                                   height={this.state.height}
                                                   width={this.state.width}/>;
        break;
      default:
        throw("Error: Widget type '" + this.state.type + "' not supported");
    }

    return visualization;
  },
  _getUrlPath() {
    if (this._isBoundToStream()) {
      return "/streams/" + this.state.config.stream_id + "/messages";
    } else {
      return "/search";
    }
  },
  _getUrlQueryString() {
    var rangeType = this.state.config['range_type'];

    var query = {
      q: this.state.config.query,
      rangetype: rangeType,
      interval: this.state.config.interval
    };
    switch(rangeType) {
      case 'relative':
        query[rangeType] = this.state.config.range;
        break;
      case 'absolute':
        query["from"] = this.state.config.from;
        query["to"] = this.state.config.to;
        break;
      case 'keyword':
        query[rangeType] = this.state.config[rangeType];
        break;
    }

    return Qs.stringify(query);
  },
  replayUrl() {
    var path = this._getUrlPath();
    var queryString = this._getUrlQueryString();

    return URLUtils.appPrefixed(path + "?" + queryString);
  },
  metricsUrl() {
    var url = "/system/metrics/master";
    var query = {
      prefilter: "org.graylog2.dashboards.widgets.*." + this.props.widgetId
    };

    return URLUtils.appPrefixed(url + "?" + Qs.stringify(query));
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
    dashboardGrid.disable();
  },
  _enableGridster() {
    dashboardGrid.enable();
  },
  updateWidget(newWidgetData) {
    newWidgetData.id = this.props.widgetId;

    WidgetsStore.updateWidget(this.props.dashboardId, newWidgetData);
    this.setState({
      title: newWidgetData.title,
      cacheTime: newWidgetData.cacheTime,
      config: newWidgetData.config
    });
  },
  deleteWidget() {
    if (window.confirm("Do you really want to delete '" + this.state.title + "'?")) {
      this.setState({deleted: true});
      $(".dashboard").trigger("delete.graylog.widget", {widgetId: this.props.widgetId});
    }
  },
  render() {
    var showConfigModal = (
      <WidgetConfigModal ref="configModal"
                         widget={this._getWidgetData()}
                         boundToStream={this._isBoundToStream()}
                         metricsAction={this._goToWidgetMetrics}/>
    );

    var editConfigModal = (
      <WidgetEditConfigModal ref="editModal"
                             widgetTypes={this.constructor.Type}
                             widget={this._getWidgetData()}
                             onUpdate={this.updateWidget}
                             onModalHidden={this._enableGridster}/>
    );

    return (
      <div ref="widget" className="widget" data-widget-id={this.props.widgetId}>
        <WidgetHeader ref="widgetHeader"
                      title={this.state.title}
                      calculatedAt={this.state.calculatedAt}
                      error={this.state.error}
                      errorMessage={this.state.errorMessage}/>

        {this._getVisualization()}

        <WidgetFooter ref="widgetFooter"
                      locked={this.state.locked}
                      onReplaySearch={this._replaySearch}
                      onShowConfig={this._showConfig}
                      onEditConfig={this._showEditConfig}
                      onDelete={this.deleteWidget}/>
        {this.state.locked ? showConfigModal : editConfigModal}
      </div>
    );
  }
});

export default Widget;
