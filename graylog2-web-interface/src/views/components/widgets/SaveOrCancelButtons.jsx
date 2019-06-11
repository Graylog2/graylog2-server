// @flow strict
import * as React from 'react';
import { Button } from 'react-bootstrap';

type Props = {
  onCancel: () => void,
  onFinish: () => void,
};

const SaveOrCancelButtons = ({ onFinish, onCancel }: Props) => (
  <React.Fragment>
    <Button onClick={onFinish} bsStyle="primary">Save</Button>
    <Button onClick={onCancel}>Cancel</Button>
  </React.Fragment>
);

export default SaveOrCancelButtons;
