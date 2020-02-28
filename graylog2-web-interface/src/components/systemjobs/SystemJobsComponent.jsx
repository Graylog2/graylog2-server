import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'components/graylog';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import { Spinner } from 'components/common';
import { SystemJobsList } from 'components/systemjobs';

const { SystemJobsStore, SystemJobsActions } = CombinedProvider.get('SystemJobs');

const SystemJobsComponent = ({ jobs }) => {
  useEffect(() => {
    SystemJobsActions.list();
    const interval = setInterval(SystemJobsActions.list, 2000);

    return () => {
      clearInterval(interval);
    };
  }, []);

  if (!jobs) {
    return <Spinner />;
  }
  const jobList = Object.keys(jobs)
    .map(nodeId => (jobs[nodeId] ? jobs[nodeId].jobs : []))
    .reduce((a, b) => a.concat(b), []);
  return (
    <Row className="content">
      <Col md={12}>
        <h2>System jobs</h2>
        <p className="description">
          A system job is a long-running task a graylog-server node executes for maintenance reasons. Some jobs
          provide progress information or can be stopped.
        </p>

        <SystemJobsList jobs={jobList} />
      </Col>
    </Row>
  );
};

SystemJobsComponent.propTypes = {
  jobs: PropTypes.objectOf(
    PropTypes.shape({
      jobs: PropTypes.array,
    }),
  ),
};

SystemJobsComponent.defaultProps = {
  jobs: undefined,
};

export default connect(SystemJobsComponent,
  { systemJobsStore: SystemJobsStore },
  ({ systemJobsStore }) => ({ jobs: systemJobsStore.jobs }));
