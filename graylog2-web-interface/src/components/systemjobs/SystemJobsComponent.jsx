import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Col, Row } from 'components/graylog';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

import { Spinner } from 'components/common';
import { SystemJobsList } from 'components/systemjobs';

const SystemJobsStore = StoreProvider.getStore('SystemJobs');
const SystemJobsActions = ActionsProvider.getActions('SystemJobs');

const SystemJobsComponent = createReactClass({
  displayName: 'SystemJobsComponent',
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
      .map(nodeId => (this.state.jobs[nodeId] ? this.state.jobs[nodeId].jobs : []))
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
