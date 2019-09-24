import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import $ from 'jquery';
import Reflux from 'reflux';

import {
  WidgetConfigModal,
  WidgetEditConfigModal,
  WidgetFooter,
  WidgetHeader,
  WidgetVisualizationNotFound,
} from 'components/widgets';
import { Icon } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';
import StoreProvider from 'injection/StoreProvider';
import Routes from 'routing/Routes';
import EventHandlersThrottler from 'util/EventHandlersThrottler';
import PermissionsMixin from 'util/PermissionsMixin';
import CombinedProvider from '../../injection/CombinedProvider';

const { WidgetsStore, WidgetsActions } = CombinedProvider.get('Widgets');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const Widget = createReactClass({
  displayName: 'Widget',

  propTypes: {
    widget: PropTypes.object.isRequired,
    dashboardId: PropTypes.string.isRequired,
    shouldUpdate: PropTypes.bool.isRequired,
    locked: PropTypes.bool.isRequired,
    streamIds: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  getInitialState() {
    const { widget } = this.props;
    this.widgetPlugin = this._getWidgetPlugin(widget.type);
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
    const { widget } = this.props;
    this.loadValueInterval = setInterval(this._loadValue, Math.min(widget.cache_time * 1000, this.DEFAULT_WIDGET_VALUE_REFRESH));

    // We need an instance per element of the widget component, so all elements get resized
    this.eventsThrottler = new EventHandlersThrottler();

    // We need to handle the event using jQuery, as the native `addEventListener` does not allow the same function
    // to be used as event handler more than once. This means that when n widgets are mounted, the same function
    // is going to be called n times on the same widget, resulting in a single widget being updated n times.
    $(window).on('resize', this._onResize);
  },

  componentWillReceiveProps(nextProps) {
    this.widgetPlugin = this._getWidgetPlugin(nextProps.widget.type);
  },

  componentDidUpdate() {
    const { deleted } = this.state;
    if (!deleted) {
      this._calculateWidgetSize();
    }
  },

  componentWillUnmount() {
    clearInterval(this.loadValueInterval);
    $(window).off('resize', this._onResize);
  },

  DEFAULT_WIDGET_VALUE_REFRESH: 10 * 1000,
  WIDGET_HEADER_HEIGHT: 25,
  WIDGET_FOOTER_HEIGHT: 20,

  _getWidgetPlugin(widgetType) {
    return PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === widgetType.toUpperCase())[0];
  },

  _isBoundToStream() {
    const { widget } = this.props;
    return ('stream_id' in widget.config) && (widget.config.stream_id !== null);
  },

  _getWidgetNode() {
    return this.node;
  },

  _loadValue() {
    const { result, deleted } = this.state;
    const { widget, shouldUpdate, dashboardId } = this.props;
    if (deleted || (result !== undefined && !shouldUpdate)) {
      return;
    }

    const width = this.node.clientWidth;

    WidgetsStore.loadValue(dashboardId, widget.id, width).then((value) => {
      // Avoid updating state if the result didn't change
      const { calculatedAt } = this.state;
      if (value.calculated_at === calculatedAt) {
        return;
      }

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
      this.setState(state => ({
        result: state.result === undefined ? 'N/A' : state.result,
        error: true,
        errorMessage: `Error loading widget value: ${error}`,
      }));
    });
  },

  _onResize() {
    const { widget } = this.props;
    this.eventsThrottler.throttle(this._calculateWidgetSize, undefined, widget.id);
  },

  _calculateWidgetSize() {
    const widgetNode = this._getWidgetNode();
    if (!widgetNode) { return; }
    const $widgetNode = $(widgetNode);
    // .height() give us the height of the whole widget without counting paddings, we need to remove the size
    // of the header and footer from that.
    const availableHeight = $widgetNode.height() - (this.WIDGET_HEADER_HEIGHT + this.WIDGET_FOOTER_HEIGHT);
    const availableWidth = $widgetNode.width();
    const { height, width } = this.state;
    if (availableHeight !== height || availableWidth !== width) {
      this.setState({ height: availableHeight, width: availableWidth });
    }
  },

  _getVisualization() {
    const { widget, locked } = this.props;
    if (widget.type === '') {
      return null;
    }

    const { result, computationTimeRange, height, width } = this.state;
    if (result === undefined) {
      return (
        <div className="loading">
          <Icon name="refresh" spin size="3x" className="spinner" />
        </div>
      );
    }

    if (result === 'N/A') {
      return <div className="not-available">{result}</div>;
    }

    if (!this.widgetPlugin) {
      return <WidgetVisualizationNotFound widgetClassName={widget.type} />;
    }

    return React.createElement(this.widgetPlugin.visualizationComponent, {
      id: widget.id,
      config: widget.config,
      data: result,
      height: height,
      width: width,
      computationTimeRange: computationTimeRange,
      locked: !locked, // widget should be locked when dashboard is unlocked and widgets can be moved
    });
  },

  _getTimeRange() {
    const { widget: { config } } = this.props;
    const rangeType = config.timerange.type;

    const timeRange = {
      rangetype: rangeType,
    };
    switch (rangeType) {
      case 'relative':
        timeRange[rangeType] = config.timerange.range;
        break;
      case 'absolute':
        timeRange.from = config.timerange.from;
        timeRange.to = config.timerange.to;
        break;
      case 'keyword':
        timeRange[rangeType] = config.timerange.keyword;
        break;
      default:
      // do nothing
    }

    return timeRange;
  },

  replayUrl() {
    const { widget: { config } } = this.props;
    if (this._isBoundToStream()) {
      return Routes.stream_search(config.stream_id, config.query, this._getTimeRange(), config.interval);
    }

    return Routes.search(config.query, this._getTimeRange(), config.interval);
  },

  _showConfig() {
    this.configModal.open();
  },

  _showEditConfig() {
    this.editModal.open();
  },

  updateWidget(newWidgetData) {
    const realNewWidgetData = newWidgetData;
    const { widget, dashboardId } = this.props;
    realNewWidgetData.id = widget.id;

    WidgetsStore.updateWidget(dashboardId, realNewWidgetData);
  },

  deleteWidget() {
    const { widget, dashboardId } = this.props;
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete "${widget.description}"?`)) {
      this.setState({ deleted: true });
      WidgetsActions.removeWidget(dashboardId, widget.id);
    }
  },

  _stopPropagation(e) {
    e.stopPropagation();
  },

  render() {
    const { currentUser, errorMessage, deleted, error, calculatedAt } = this.state;
    if (deleted) {
      return <span />;
    }
    const { widget, locked, streamIds, dashboardId } = this.props;
    const showConfigModal = (
      <WidgetConfigModal ref={(node) => { this.configModal = node; }}
                         dashboardId={dashboardId}
                         widget={widget}
                         boundToStream={this._isBoundToStream()} />
    );

    const editConfigModal = (
      <WidgetEditConfigModal ref={(node) => { this.editModal = node; }}
                             widget={widget}
                             onUpdate={this.updateWidget} />
    );

    /* Note that we consider two cases here: a dashboard configured from
       a stream and a dashboard configured from global search. */
    const canReadConfiguredStream = widget.config.stream_id && streamIds != null
      && streamIds[widget.config.stream_id];
    const canSearchGlobally = !widget.config.stream_id
      && this.isPermitted(currentUser.permissions,
        ['searches:absolute', 'searches:keyword', 'searches:relative']);

    const disabledReplay = !canReadConfiguredStream && !canSearchGlobally;

    return (
      <div role="presentation"
           ref={(node) => { this.node = node; }}
           className="widget"
           data-widget-id={widget.id}>
        <WidgetHeader title={widget.description} />

        {this._getVisualization()}

        <WidgetFooter locked={locked}
                      onShowConfig={this._showConfig}
                      onEditConfig={this._showEditConfig}
                      onDelete={this.deleteWidget}
                      replayHref={this.replayUrl()}
                      replayDisabled={disabledReplay}
                      calculatedAt={calculatedAt}
                      error={error}
                      errorMessage={errorMessage} />
        <div role="presentation"
             onClick={this._stopPropagation}
             onMouseDown={this._stopPropagation}>
          {locked ? showConfigModal : editConfigModal}
        </div>
      </div>
    );
  },
});

export default Widget;
