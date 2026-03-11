/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { useQuery } from '@tanstack/react-query';

import { ClusterJobs } from '@graylog/server-api';

import { SystemJobsList } from 'components/systemjobs';
import { Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useProductName from 'brand-customization/useProductName';
import { defaultOnError } from 'util/conditional/onError';

export const SYSTEM_JOBS_QUERY_KEY = ['system', 'jobs'];

const SystemJobsComponent = () => {
  const productName = useProductName();
  const { data: jobs, isLoading } = useQuery({
    queryKey: SYSTEM_JOBS_QUERY_KEY,
    queryFn: () =>
      defaultOnError(ClusterJobs.list(), 'Loading system jobs failed with status', 'Could not load system jobs'),
    refetchInterval: 2000,
  });

  if (isLoading || !jobs) {
    return <Spinner />;
  }

  const jobList = Object.keys(jobs)
    .map((nodeId) => (jobs[nodeId] ? jobs[nodeId].jobs : []))
    .reduce((a, b) => a.concat(b), []);

  return (
    <Row className="content">
      <Col md={12}>
        <h2>System jobs</h2>
        <p className="description">
          A system job is a long-running task a {productName} server node executes for maintenance reasons. Some jobs
          provide progress information or can be stopped.
        </p>

        <SystemJobsList jobs={jobList} />
      </Col>
    </Row>
  );
};

export default SystemJobsComponent;
