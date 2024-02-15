import React from 'react';
import styled from 'styled-components';

import { Icon } from 'components/common';
import { Panel } from 'components/bootstrap';
import { StyledPanel } from 'components/datanode/migrations/MigrationWelcomeStep';

const StyledHelpPanel = styled(StyledPanel)`
  margin-top: 30px;
`;
const JournalSizeWarning = () => (
  <StyledHelpPanel bsStyle="warning">
    <Panel.Heading>
      <Panel.Title componentClass="h3"><Icon name="exclamation-triangle" /> Journal size warning</Panel.Title>
    </Panel.Heading>
    <Panel.Body>
      <p>Please note that during migration you will have to stop processing on your graylog node, this will result in the journal growing in size.
        Therefore you will have to increase your journal volume size during the Journal size downsize step or earlier.
      </p>
    </Panel.Body>
  </StyledHelpPanel>
);
export default JournalSizeWarning;
