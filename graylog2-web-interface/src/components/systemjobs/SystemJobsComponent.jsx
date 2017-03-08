import React from 'react';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const SystemJobsStore = StoreProvider.getStore('SystemJobs');

import ActionsProvider from 'injection/ActionsProvider';
const SystemJobsActions = ActionsProvider.getActions('SystemJobs');

import { Spinner } from 'components/common';
import { SystemJobsList } from 'components/systemjobs';

const SystemJobsComponent = React.createClass({
  mixins: [Reflux.connect(SystemJobsStore)],
  componentDidMount() {
    SystemJobsActions.list();

    this.interval = setInterval(SystemJobsActions.list, 2000);
  },
  componentWillUnmount() {
    clearInterval(this.interval);
  },
  render() {
    if (!this.state.jobs) {
      return <Spinner />;
    }
    const jobs = Object.keys(this.state.jobs)
      .map(nodeId => this.state.jobs[nodeId] ? this.state.jobs[nodeId].jobs : [])
      .reduce((a, b) => a.concat(b));
    return (
      <Row className="content">
        <Col md={12}>
          <h2>System jobs</h2>
          <p className="description">
            A system job is a long-running task a graylog-server node executes for maintenance reasons. Some jobs
            provide progress information or can be stopped.
          </p>

          <SystemJobsList jobs={jobs} />
        </Col>
      </Row>
    );
  },
});

export default SystemJobsComponent;
