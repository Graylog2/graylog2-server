import { useEffect } from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import AppConfig from 'util/AppConfig';

/**
 * This component should be conditionally rendered if you have a form that is in a "dirty" state. It will confirm with the user that they want to navigate away, refresh, or in any way unload the component.
 */
const ConfirmLeaveDialog = ({ question, router, route }) => {
  const handleLeavePage = (e) => {
    if (AppConfig.gl2DevMode()) {
      return null;
    }

    e.returnValue = question;
    return question;
  };

  const routerWillLeave = () => {
    return handleLeavePage({});
  };

  useEffect(() => {
    window.addEventListener('beforeunload', handleLeavePage);
    const unsubscribe = router.setRouteLeaveHook(route, routerWillLeave);

    return () => {
      window.removeEventListener('beforeunload', handleLeavePage);
      unsubscribe();
    };
  }, []);

  return null;
};

ConfirmLeaveDialog.propTypes = {
  /** Phrase used in the confirmation dialog. */
  question: PropTypes.string,
  /** `route` object from withRouter() HOC */
  route: PropTypes.object.isRequired,
  router: PropTypes.shape({
    setRouteLeaveHook: PropTypes.func.isRequired,
  }).isRequired,
};

ConfirmLeaveDialog.defaultProps = {
  question: 'Are you sure?',
};

/** @component */
export default withRouter(ConfirmLeaveDialog);
