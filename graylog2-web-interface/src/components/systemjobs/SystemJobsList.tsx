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
