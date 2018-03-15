import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Spinner } from 'components/common';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import { Button } from 'react-bootstrap';

import { MapsActions, MapsStore } from 'stores/maps/MapsStore';
import MapVisualization from 'components/maps/widgets/MapVisualization';
import EventHandlersThrottler from 'util/EventHandlersThrottler';
import StoreProvider from 'injection/StoreProvider';
const RefreshStore = StoreProvider.getStore('Refresh');

const FieldAnalyzerMapComponent = createReactClass({
  displayName: 'FieldAnalyzerMapComponent',

  propTypes: {
    from: PropTypes.any.isRequired,
    to: PropTypes.any.isRequired,
    resolution: PropTypes.any.isRequired,
    stream: PropTypes.object,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    query: PropTypes.string.isRequired,
    page: PropTypes.number.isRequired,
    rangeType: PropTypes.string.isRequired,
    rangeParams: PropTypes.object.isRequired,
    forceFetch: PropTypes.bool,
  },

  mixins: [Reflux.connect(MapsStore), Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer')],

  getInitialState() {
    return {
      field: undefined,
      width: this.DEFAULT_WIDTH,
    };
  },

  componentDidMount() {
    window.addEventListener('resize', this._onResize);
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._onResize);
  },

  componentWillReceiveProps(nextProps) {
    // Reload values when executed search changes
    if (this.props.query !== nextProps.query ||
        this.props.rangeType !== nextProps.rangeType ||
        JSON.stringify(this.props.rangeParams) !== JSON.stringify(nextProps.rangeParams) ||
        this.props.stream !== nextProps.stream ||
        nextProps.forceFetch) {
        this._loadData(nextProps);
    }
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
    this.setState({field: field}, () => {
      // We need to update the map width when the container is rendered
      this._updateMapWidth();
      this._loadData(this.props);
    });
  },

  _onResize() {
    this.eventThrottler.throttle(() => this._updateMapWidth());
  },

  _updateMapWidth() {
    this.setState({width: (this.refs.mapContainer ? this.refs.mapContainer.clientWidth : this.DEFAULT_WIDTH)});
  },

  _getStreamId() {
    return this.props.stream ? this.props.stream.id : null;
  },

  _loadData(props) {
    if (this.state.field !== undefined) {
      const promise = MapsActions.getMapData(
        props.query,
        this.state.field,
        props.rangeType,
        props.rangeParams,
        this._getStreamId()
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

    if (!this.state.mapCoordinates) {
      inner = <Spinner />;
    } else {
      inner = (
        <MapVisualization id="1" data={this.state.mapCoordinates} height={400} width={this.state.width} config={{}}/>
      );
    }

    if (this.state.field !== undefined) {
      content = (
        <div className="content-col">
          <div className="pull-right">
            <AddToDashboardMenu title="Add to dashboard"
                                widgetType={this.WIDGET_TYPE}
                                configuration={{field: this.state.field}}
                                pullRight
                                permissions={this.props.permissions}>

              <Button bsSize="small" onClick={() => this._resetStatus()}><i className="fa fa-close" /></Button>
            </AddToDashboardMenu>
          </div>
          <h1>Map for field: {this.state.field}</h1>

          <div ref="mapContainer" style={{maxHeight: 400, overflow: 'auto', marginTop: 10}}>{inner}</div>
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
