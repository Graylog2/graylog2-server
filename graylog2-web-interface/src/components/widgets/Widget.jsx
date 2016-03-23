import React, {PropTypes} from 'react';
import ReactDOM from 'react-dom';
import $ from 'jquery';
import Qs from 'qs';
import { PluginStore } from 'graylog-web-plugin/plugin';

import URLUtils from 'util/URLUtils';

import { WidgetConfigModal, WidgetEditConfigModal, WidgetFooter, WidgetHeader, WidgetVisualizationNotFound } from 'components/widgets';

import WidgetsStore from 'stores/widgets/WidgetsStore';
import WidgetsActions from 'actions/widgets/WidgetsActions';

const Widget = React.createClass({
  propTypes: {
    widget: PropTypes.object.isRequired,
    dashboardId: PropTypes.string.isRequired,
    shouldUpdate: PropTypes.bool.isRequired,
    locked: PropTypes.bool.isRequired,
  },
  getInitialState() {
    this.widgetPlugin = this._getWidgetPlugin(this.props.widget.type);
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
  componentWillReceiveProps(nextProps) {
    this.widgetPlugin = this._getWidgetPlugin(nextProps.widget.type);
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

  _getWidgetPlugin(widgetType) {
    return PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === widgetType.toUpperCase())[0];
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
        errorMessage: `Error loading widget value: ${error}`,
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
      this.setState({ height: availableHeight, width: availableWidth });
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

    if (!this.widgetPlugin) {
      return <WidgetVisualizationNotFound widgetClassName={this.props.widget.type}/>;
    }

    return React.createElement(this.widgetPlugin.visualizationComponent, {
      id: this.props.widget.id,
      config: this.props.widget.config,
      data: this.state.result,
      height: this.state.height,
      width: this.state.width,
      computationTimeRange: this.state.computationTimeRange,
    });
  },
  _getUrlPath() {
    if (this._isBoundToStream()) {
      return `/streams/${this.props.widget.config.stream_id}/search`;
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
      default:
        // do nothing
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
    if (window.confirm(`Do you really want to delete "${this.props.widget.description}"?`)) {
      this.setState({deleted: true});
      WidgetsActions.removeWidget(this.props.dashboardId, this.props.widget.id);
    }
  },
  render() {
    if (this.state.deleted) {
      return <span/>;
    }
    const showConfigModal = (
      <WidgetConfigModal ref="configModal"
                         dashboardId={this.props.dashboardId}
                         widget={this.props.widget}
                         boundToStream={this._isBoundToStream()}/>
    );

    const editConfigModal = (
      <WidgetEditConfigModal ref="editModal"
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
