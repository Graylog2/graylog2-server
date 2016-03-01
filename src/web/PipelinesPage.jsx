import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import { Button, Row, Col } from 'react-bootstrap';

import { LinkContainer } from 'react-router-bootstrap';

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import Spinner from 'components/common/Spinner';

import PipelineStreamComponent from 'PipelineStreamComponent';

import PipelinesActions from 'PipelinesActions';
import PipelinesStore from 'PipelinesStore';
import PipelinesComponent from 'PipelinesComponent';

const PipelinesPage = React.createClass({
  mixins: [
    Reflux.connect(PipelinesStore),
  ],
  contextTypes: {
    storeProvider: React.PropTypes.object,
  },
  getInitialState() {
    return {
      pipelines: undefined,
      streams: undefined,
      assignments: [],
    }
  },

  componentDidMount() {
    PipelinesActions.list();

    var store = this.context.storeProvider.getStore('Streams');
    store.listStreams().then((streams) => this.setState({streams: streams}));
  },

  render() {
    let content;
    if (!this.state.pipelines || !this.state.streams) {
      content = <Spinner />;
    } else {
      content = [

      ];
    }
    return (
      <span>
        <PageHeader title="Processing pipelines">
          <span>Pipelines define how Graylog processes data by grouping rules into stages. Pipelines can apply to all incoming messages or only to messages on a certain stream.</span>
          <span>
            Read more about Graylog pipelines in the <DocumentationLink page={"TODO"} text="documentation"/>.
          </span>

          <LinkContainer to={'/system/pipelines/rules'}>
            <Button bsStyle="info">Configure rules</Button>
          </LinkContainer>
        </PageHeader>
        {content}
      </span>);
  },
});

export default PipelinesPage;
