import React from 'react';
import styled from 'styled-components';

import { Table } from 'components/graylog';
import { Icon } from 'components/common';
import { SectionTitle, SectionNote } from 'aws/common/sharedStyles';

const StyledTable = styled(Table)`
  margin: 0;
`;

const Automatic = () => {
  return (
    <StyledTable condensed>
      <thead>
        <tr>
          <td colSpan="2">
            <SectionTitle>Automatic authentication will attempt each of the following in the listed order.</SectionTitle>
          </td>
        </tr>
      </thead>

      <tbody>
        <tr>
          <th>Environment variables</th>
          <td><code>AWS_ACCESS_KEY_ID</code> and <code>AWS_SECRET_ACCESS_KEY</code></td>
        </tr>
        <tr>
          <th>Java system properties</th>
          <td><code>aws.accessKeyId</code> and <code>aws.secretKey</code></td>
        </tr>
        <tr>
          <th>Default credential profiles file</th>
          <td>Typically located at <code>~/.aws/credentials</code></td>
        </tr>
        <tr>
          <th>Amazon ECS container credentials</th>
          <td>Loaded from the Amazon ECS if the environment variable <code>AWS_CONTAINER_CREDENTIALS_RELATIVE_URI</code> is set</td>
        </tr>
        <tr>
          <th>Instance profile credentials</th>
          <td>Used on EC2 instances, and delivered through the Amazon EC2 metadata service</td>
        </tr>
      </tbody>

      <tfoot>
        <tr>
          <td colSpan="2">
            <SectionNote>
              For more information, check out the <a href="https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html" target="_blank" rel="noopener noreferrer">AWS Credential Configuration Documentation <Icon name="external-link" /></a>
            </SectionNote>
          </td>
        </tr>
      </tfoot>
    </StyledTable>
  );
};

export default Automatic;
