// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import ConfirmLeaveDialog from 'components/common/ConfirmLeaveDialog';
import { ViewStore } from 'views/stores/ViewStore';

type Props = {
  dirty: boolean,
};

const WindowLeaveMessage = ({ dirty }: Props) => (dirty
  ? (
    <ConfirmLeaveDialog question="Are you sure you want to leave the page? Any unsaved changes will be lost." />
  )
  : null);

WindowLeaveMessage.propTypes = {
  dirty: PropTypes.bool.isRequired,
};

export default connect(WindowLeaveMessage, { view: ViewStore }, ({ view }) => ({ dirty: view.dirty }));
