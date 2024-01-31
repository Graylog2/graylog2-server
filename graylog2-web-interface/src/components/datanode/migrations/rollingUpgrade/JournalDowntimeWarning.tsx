import React from 'react';
import styled from 'styled-components';

import type { MigrationActions } from 'components/datanode/Types';
import { Alert, Button } from 'components/bootstrap';

type Props = {
  onStepComplete: (step: {step: MigrationActions}) => void,
  nextSteps: Array<MigrationActions>
};

const DownsizeWarning = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const JournalDowntimeWarning = ({ onStepComplete, nextSteps }: Props) => (
  <>
    <DownsizeWarning bsStyle="danger">
      <h4>During the next step the journal size will increase because of stopping the processing.</h4>
      <ul>
        <li>Current journal size: 1Go</li>
        <li>Messages: 10 000</li>
        <li>Volume size:  2Go</li>
        <li><b>Estimated down time: 5mn</b></li>
      </ul>
      <p>Please increase you journal volume size before proceeding.</p>
    </DownsizeWarning>
    <Button bsStyle="primary" bsSize="small" onClick={() => onStepComplete({ step: nextSteps[0] })}>Next</Button>
  </>
);
export default JournalDowntimeWarning;
