import React from 'react';
import styled, { css } from 'styled-components';

import { Alert } from 'components/bootstrap';
import { ExternalLink } from 'components/common';

const StyledOl = styled.ol(
  ({ theme }) => css`
    padding-left: 20px;

    > li {
      margin-bottom: ${theme.spacings.sm};
    }
    > li:last-child {
      margin-bottom: 0;
    }
  `,
);

const GCSSetupInfo = () => (
  <Alert bsStyle="info">
    <p>To setup a Google Cloud Storage backend, the steps are as follows: </p>
    <StyledOl>
      <li>
        Create a Google Cloud Storage Bucket with a unique name - see Google&lsquo;s documentation on{' '}
        <ExternalLink href="https://cloud.google.com/storage/docs/creating-buckets">Buckets</ExternalLink>. The default
        Standard Storage Class is recommended.
      </li>
      <li>
        Create a Google Cloud Service Account, with permissions to read/write/delete from that Bucket - see
        Google&lsquo;s documentation on{' '}
        <ExternalLink href="https://cloud.google.com/iam/docs/service-account-overview">Service Accounts</ExternalLink>.
      </li>
      <li>
        Set up Application Default credentials on all Graylog nodes. The method differs depending on how your cluster is
        hosted - see Google&lsquo;s documentation on{' '}
        <ExternalLink href="https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to">
          Setting up Application Default credentials
        </ExternalLink>
        .
      </li>
      <li>
        On this page, you are now ready to set up a Google Cloud Storage Backend. You will need to provide the unique
        name of the Google Cloud storage created during the first step.
      </li>
    </StyledOl>
  </Alert>
);

export default GCSSetupInfo;
