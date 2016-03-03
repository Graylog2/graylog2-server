import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import { Button, Row, Col } from 'react-bootstrap';

import { LinkContainer } from 'react-router-bootstrap';

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import Spinner from 'components/common/Spinner';

import PipelinesActions from 'pipelines/PipelinesActions';
import PipelinesStore from 'pipelines/PipelinesStore';

const PipelinesInputsPage = React.createClass({
  contextTypes: {
    storeProvider: PropTypes.object,
  },

  mixins: [
    Reflux.connect(PipelinesStore),
  ],

  getInitialState() {
    return {
      pipelines: undefined,
      streams: undefined,
      assignments: [],
    }
  },

  componentDidMount() {
    PipelinesActions.list();

    const store = this.context.storeProvider.getStore('Streams');
    store.listStreams().then((streams) => this.setState({streams: streams}));
  },

  render() {
    let content;
    if (!this.state.pipelines || !this.state.streams) {
      content = <Spinner />;
    } else {
      content = [];
    }
    return (
      <span>
        <PageHeader title="Processing pipelines" titleSize={9} buttonSize={3}>
          <span>Pipelines define how Graylog processes data by grouping rules into stages. Pipelines can apply to all incoming messages or only to messages on a certain stream.</span>
          <span>
            Read more about Graylog pipelines in the <DocumentationLink page={"TODO"} text="documentation"/>.
          </span>

          <span>
            <LinkContainer to={'/system/pipelines/overview'}>
              <Button bsStyle="info">Manage pipelines</Button>
            </LinkContainer>
            {' '}
            <LinkContainer to={'/system/pipelines/rules'}>
              <Button bsStyle="info">Manage rules</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {content}
          </Col>
        </Row>
      </span>);
  },
});

export default PipelinesInputsPage;
