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
import styled, { css } from 'styled-components';

import { SystemJob } from 'components/systemjobs';
import { Alert } from 'components/bootstrap';

const SystemJobWrap = styled.div(({ theme }) => css`
  margin-top: 10px;
  border-bottom: 1px solid ${theme.colors.gray[80]};

  .progress {
    margin-top: 2px;
    margin-bottom: 5px;
  }

  .finished {
    display: none;
    position: relative;
    top: -1px;
    left: 2px;
    font-family: Arial, sans-serif;
    font-size: ${theme.fonts.size.small};
  }
`);

const StyledAlert = styled(Alert)`
  margin-top: 10px;
`;

type SystemJobsListProps = {
  jobs: any[];
};

const SystemJobsList = ({
  jobs,
}: SystemJobsListProps): React.ReactElement => {
  const formatSystemJob = (job) => (
    <SystemJobWrap key={`job-${job.id}`}>
      <SystemJob job={job} />
    </SystemJobWrap>
  );

  const formattedJobs = jobs.map(formatSystemJob);

  return (
    (formattedJobs.length === 0)
      ? (
        <StyledAlert bsStyle="info">
          No active system jobs.
        </StyledAlert>
      )
      : (
        <span>
          {formattedJobs}
        </span>
      )
  );
};

export default SystemJobsList;
