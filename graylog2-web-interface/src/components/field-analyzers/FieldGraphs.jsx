import React, {PropTypes} from 'react';
import ReactDOM from 'react-dom';

import LegacyFieldGraph from './LegacyFieldGraph';
import FieldGraphsStore from 'stores/field-analyzers/FieldGraphsStore';
import UIUtils from 'util/UIUtils';

const FieldGraphs = React.createClass({
  propTypes: {
    from: PropTypes.any.isRequired,
    to: PropTypes.any.isRequired,
    resolution: PropTypes.any.isRequired,
    searchInStream: PropTypes.bool,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
  },
  getInitialState() {
    this.notifyOnNewGraphs = false;

    return {
      fieldGraphs: FieldGraphsStore.fieldGraphs,
      stackedGraphs: FieldGraphsStore.stackedGraphs,
    };
  },
  componentDidMount() {
    this.initialFieldGraphs = this.state.fieldGraphs;
    this.notifyOnNewGraphs = true;

    FieldGraphsStore.onFieldGraphsUpdated = (newFieldGraphs) => this.setState({fieldGraphs: newFieldGraphs});
    FieldGraphsStore.onFieldGraphsMerged = (newStackedGraphs) => this.setState({stackedGraphs: newStackedGraphs});
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
  addFieldGraph(field) {
    const streamId = this.props.searchInStream ? this.props.searchInStream.id : undefined;
    FieldGraphsStore.newFieldGraph(field, {interval: this.props.resolution, streamid: streamId});
  },
  deleteFieldGraph(graphId) {
    FieldGraphsStore.deleteGraph(graphId);
  },
  render() {
    const fieldGraphs = [];

    this.state.fieldGraphs
      .sortBy(graph => graph.createdAt)
      .forEach((graphOptions, graphId) => {
        fieldGraphs.push(
          <LegacyFieldGraph key={graphId}
                            ref={graphId}
                            graphId={graphId}
                            graphOptions={graphOptions}
                            onDelete={() => this.deleteFieldGraph(graphId)}
                            from={this.props.from}
                            to={this.props.to}
                            permissions={this.props.permissions}
                            stacked={this.state.stackedGraphs.has(graphId)}
                            hidden={this.state.stackedGraphs.some((stackedGraphs) => stackedGraphs.has(graphId))}/>
        );
      });

    return (
      <div id="field-graphs">
        {fieldGraphs}
      </div>
    );
  },
});

export default FieldGraphs;
