import React from 'react';
import Reflux from 'reflux';
import { Spinner } from 'components/common';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import { Button } from 'react-bootstrap';

import { MapsActions, MapsStore } from 'stores/MapsStore';
import MapVisualization from 'components/MapVisualization';

const FieldAnalyzerMapComponent = React.createClass({
  propTypes: {
    from: React.PropTypes.any.isRequired,
    to: React.PropTypes.any.isRequired,
    resolution: React.PropTypes.any.isRequired,
    stream: React.PropTypes.object,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    query: React.PropTypes.string.isRequired,
    page: React.PropTypes.number.isRequired,
    rangeType: React.PropTypes.string.isRequired,
    rangeParams: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(MapsStore)],

  getInitialState() {
    return {
      field: undefined,
    };
  },

  WIDGET_TYPE: 'org.graylog.plugins.map.widget.strategy.MapWidgetStrategy',

  addField(field) {
    this.setState({field: field}, () => this._loadData());
  },

  _getStreamId() {
    return this.props.stream ? this.props.stream.id : null;
  },

  _loadData() {
    if (this.state.field !== undefined) {
      const promise = MapsActions.getMapData(
        this.props.query,
        this.state.field,
        this.props.rangeType,
        this.props.rangeParams,
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
        <MapVisualization id="1" data={this.state.mapCoordinates} height={400} width={1200} config={{}}/>
      );
    }

    if (this.state.field !== undefined) {
      content = (
        <div className="content-col">
          <div className="pull-right">
            <AddToDashboardMenu title="Add to dashboard"
                                widgetType={this.WIDGET_TYPE}
                                configuration={{field: this.state.field}}
                                bsStyle="default"
                                pullRight
                                permissions={this.props.permissions}>
              <Button bsSize="small" onClick={() => this._resetStatus()}>Dismiss</Button>
            </AddToDashboardMenu>
          </div>
          <h1>Map for field: {this.state.field}</h1>

          <div style={{maxHeight: 400, overflow: 'auto', marginTop: 10}}>{inner}</div>
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
