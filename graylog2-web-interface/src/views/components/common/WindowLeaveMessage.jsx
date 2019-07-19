// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
// $FlowFixMe: imports from core need to be fixed in flow
import { withRouter } from 'react-router';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

import ConfirmLeaveDialog from 'components/common/ConfirmLeaveDialog';

import { ViewStore } from 'views/stores/ViewStore';


type Props = {
  dirty: boolean,
  route: any,
};

class WindowLeaveMessage extends React.PureComponent<Props> {
  render() {
    const { dirty, route } = this.props;

    return dirty
      ? (
        <ConfirmLeaveDialog route={route}
                            question="Are you sure you want to leave the page? Any unsaved changes will be lost." />
      )
      : null;
  }
}

WindowLeaveMessage.propTypes = {
  dirty: PropTypes.bool.isRequired,
  route: PropTypes.object.isRequired,
};

export default connect(withRouter(WindowLeaveMessage), { view: ViewStore }, ({ view }) => ({ dirty: view.dirty }));
