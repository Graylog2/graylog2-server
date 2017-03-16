import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import Immutable from 'immutable';
import PureRenderMixin from 'react-addons-pure-render-mixin';

import LegacyFieldGraph from './LegacyFieldGraph';

import StoreProvider from 'injection/StoreProvider';
const FieldGraphsStore = StoreProvider.getStore('FieldGraphs');

import UIUtils from 'util/UIUtils';

const FieldGraphs = React.createClass({
  propTypes: {
    from: PropTypes.any.isRequired,
    to: PropTypes.any.isRequired,
    resolution: PropTypes.any.isRequired,
    stream: PropTypes.object,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
  },
  mixins: [PureRenderMixin],
  getInitialState() {
    this.notifyOnNewGraphs = false;

    return {
      fieldGraphs: Immutable.fromJS(FieldGraphsStore.fieldGraphs.toJS()),
      stackedGraphs: Immutable.fromJS(FieldGraphsStore.stackedGraphs.toJS()),
    };
  },
  componentDidMount() {
    this.initialFieldGraphs = this.state.fieldGraphs;
    this.notifyOnNewGraphs = true;

    FieldGraphsStore.onFieldGraphsUpdated = newFieldGraphs => this.setState({ fieldGraphs: Immutable.fromJS(newFieldGraphs.toJS()) });
    FieldGraphsStore.onFieldGraphsMerged = newStackedGraphs => this.setState({ stackedGraphs: Immutable.fromJS(newStackedGraphs.toJS()) });
    FieldGraphsStore.onFieldGraphCreated = (graphId) => {
      if (this.notifyOnNewGraphs && !this.initialFieldGraphs.has(graphId)) {
        const element = ReactDOM.findDOMNode(this.refs[graphId]);
        UIUtils.scrollToHint(element);
      }
    };
  },
  componentWillUnmount() {
    FieldGraphsStore.resetStore();
  },
  addField(field) {
    const streamId = this.props.stream ? this.props.stream.id : undefined;
    FieldGraphsStore.newFieldGraph(field, { interval: this.props.resolution, streamid: streamId });
  },
  deleteFieldGraph(graphId) {
    FieldGraphsStore.deleteGraph(graphId);
  },
  render() {
    const fieldGraphs = this.state.fieldGraphs
      .sortBy(graph => graph.createdAt)
      .map((graphOptions, graphId) =>
        <LegacyFieldGraph key={graphId}
                            ref={graphId}
                            graphId={graphId}
                            graphOptions={graphOptions.toJS()}
                            onDelete={() => this.deleteFieldGraph(graphId)}
                            from={this.props.from}
                            to={this.props.to}
                            permissions={this.props.permissions}
                            stacked={this.state.stackedGraphs.has(graphId)}
                            hidden={this.state.stackedGraphs.some(stackedGraphs => stackedGraphs.has(graphId))} />,
      );

    return (
      <div id="field-graphs">
        {fieldGraphs.valueSeq()}
      </div>
    );
  },
});

export default FieldGraphs;
