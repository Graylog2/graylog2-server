import React from 'react';
import styled, { css } from 'styled-components';

import { Panel } from 'components/bootstrap';

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(({ bsStyle = 'default', theme }) => css`
  &.panel {
    background-color: ${theme.colors.global.contentBackground};
    .panel-heading {
      color: ${theme.colors.variant.darker[bsStyle]};
    }
  }
  margin-top: ${theme.spacings.md} !important;
`);

const ImPlaceMigrationInfo = () => (
  <StyledPanel bsStyle="info">
    <Panel.Heading>
      <Panel.Title componentClass="h3">In-Place migration</Panel.Title>
    </Panel.Heading>
    <Panel.Body>
      If you are doing an In-Place migration, make sure that the configuration of your Data nodes in <code>datanode.conf</code>, specifically the <code>opensearch_data_location</code> configuration option, points to the correct existing OpenSearch data directory on every node.
    </Panel.Body>
  </StyledPanel>
);
export default ImPlaceMigrationInfo;
