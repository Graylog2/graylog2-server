import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';

import PipelinesActions from 'PipelinesActions';
import PipelinesStore from 'PipelinesStore';

const PipelinesPage = React.createClass({
  mixins: [Reflux.connect(PipelinesStore)],

  getInitialState() {
    return {
      pipelines: undefined,
    }
  },

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    PipelinesActions.list();
  },

  render() {
    let content;
    if (!this.state.pipelines) {
      content = <Spinner />;
    } else {
      content = this.state.pipelines.length;
    }
    return (
      <span>
        <PageHeader title="Processing pipelines">
          <span>Processing pipelines</span>
        </PageHeader>
        <Row className="content">
          <Col md={12}>
            <span>{content}</span>
          </Col>
        </Row>
      </span>);
  },
});

export default PipelinesPage;
