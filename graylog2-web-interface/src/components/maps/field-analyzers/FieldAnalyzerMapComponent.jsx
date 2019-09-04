import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import { Button, Icon } from 'components/graylog';

import { MapsActions, MapsStore } from 'stores/maps/MapsStore';
import MapVisualization from 'components/maps/widgets/MapVisualization';
import EventHandlersThrottler from 'util/EventHandlersThrottler';
import StoreProvider from 'injection/StoreProvider';

const RefreshStore = StoreProvider.getStore('Refresh');

const FieldAnalyzerMapComponent = createReactClass({
  displayName: 'FieldAnalyzerMapComponent',

  propTypes: {
    stream: PropTypes.object,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    query: PropTypes.string.isRequired,
    rangeType: PropTypes.string.isRequired,
    rangeParams: PropTypes.object.isRequired,
    forceFetch: PropTypes.bool,
  },

  mixins: [Reflux.connect(MapsStore), Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer')],

  getDefaultProps() {
    return {
      stream: undefined,
      forceFetch: false,
    };
  },

  getInitialState() {
    return {
      field: undefined,
      width: this.DEFAULT_WIDTH,
    };
  },

  componentDidMount() {
    window.addEventListener('resize', this._onResize);
  },

  componentWillReceiveProps(nextProps) {
    const { query, rangeParams, rangeType, stream } = this.props;
    // Reload values when executed search changes
    if (query !== nextProps.query
        || rangeType !== nextProps.rangeType
        || JSON.stringify(rangeParams) !== JSON.stringify(nextProps.rangeParams)
        || stream !== nextProps.stream
        || nextProps.forceFetch) {
      this._loadData(nextProps);
    }
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._onResize);
  },

  _setupTimer(refresh) {
    this._stopTimer();
    if (refresh.enabled) {
      this.timer = setInterval(() => this._loadData(this.props), refresh.interval);
    }
  },

  _stopTimer() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },

  DEFAULT_WIDTH: 800,
  WIDGET_TYPE: 'org.graylog.plugins.map.widget.strategy.MapWidgetStrategy',
  eventThrottler: new EventHandlersThrottler(),

  addField(field) {
    this.setState({ field: field }, () => {
      // We need to update the map width when the container is rendered
      this._updateMapWidth();
      this._loadData(this.props);
    });
  },

  _onResize() {
    this.eventThrottler.throttle(() => this._updateMapWidth());
  },

  _updateMapWidth() {
    this.setState({ width: (this.mapContainer ? this.mapContainer.clientWidth : this.DEFAULT_WIDTH) });
  },

  _getStreamId() {
    const { stream } = this.props;
    return stream ? stream.id : null;
  },

  _loadData(props) {
    const { field } = this.state;
    if (field !== undefined) {
      const promise = MapsActions.getMapData(
        props.query,
        field,
        props.rangeType,
        props.rangeParams,
        this._getStreamId(),
      );
      promise.catch(() => this._resetStatus());
    }
  },

  _resetStatus() {
    this.setState(this.getInitialState());
  },

  render() {
    let content;
    let inner;
    const { field, mapCoordinates, width } = this.state;
    const { permissions } = this.props;

    if (!mapCoordinates) {
      inner = <Spinner />;
    } else {
      inner = (
        <MapVisualization id="1" data={mapCoordinates} height={400} width={width} config={{}} />
      );
    }

    if (field !== undefined) {
      content = (
        <div className="content-col">
          <div className="pull-right">
            <AddToDashboardMenu title="Add to dashboard"
                                widgetType={this.WIDGET_TYPE}
                                configuration={{ field: field }}
                                pullRight
                                permissions={permissions}>

              <Button bsSize="small" onClick={() => this._resetStatus()}><Icon className="fa fa-close" /></Button>
            </AddToDashboardMenu>
          </div>
          <h1>Map for field: {field}</h1>

          <div ref={(mapContainer) => { this.mapContainer = mapContainer; }} style={{ maxHeight: 400, overflow: 'auto', marginTop: 10 }}>{inner}</div>
        </div>
      );
    }
    return (
      <div id="field-quick-values">
        {content}
      </div>
    );
  },
});

export default FieldAnalyzerMapComponent;
