import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import { Row, Col } from 'react-bootstrap';
import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';

import PipelineStreamComponent from 'PipelineStreamComponent';

import PipelinesActions from 'PipelinesActions';
import PipelinesStore from 'PipelinesStore';
import PipelinesComponent from 'PipelinesComponent';

import RulesActions from 'RulesActions';
import RulesStore from 'RulesStore';
import RulesComponent from 'RulesComponent';


const PipelinesPage = React.createClass({
  mixins: [
    Reflux.connect(PipelinesStore),
    Reflux.connect(RulesStore),
  ],
  contextTypes: {
    storeProvider: React.PropTypes.object,
  },
  getInitialState() {
    return {
      pipelines: undefined,
      rules: undefined,
      streams: undefined,
      assignments: [],
    }
  },

  componentDidMount() {
    PipelinesActions.list();
    RulesActions.list();

    var store = this.context.storeProvider.getStore('Streams');
    store.listStreams().then((streams) => this.setState({streams: streams}));
  },

  render() {
    let content;
    if (!this.state.pipelines || !this.state.rules || !this.state.streams) {
      content = <Spinner />;
    } else {
      content = [
        <PipelineStreamComponent key="assignments" pipelines={this.state.pipelines} streams={this.state.streams} assignments={this.state.assignments}/>,
        <PipelinesComponent key="pipelines" pipelines={this.state.pipelines} />,
        <RulesComponent key="rules" rules={this.state.rules} />
      ];
    }
    return (
      <span>
        <PageHeader title="Processing pipelines">
          <span>Processing pipelines</span>
        </PageHeader>
        {content}
      </span>);
  },
});

export default PipelinesPage;
