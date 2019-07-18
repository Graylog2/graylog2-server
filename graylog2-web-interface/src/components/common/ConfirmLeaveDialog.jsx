import { useEffect } from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import AppConfig from 'util/AppConfig';

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
  question: PropTypes.string,
  route: PropTypes.object.isRequired,
  router: PropTypes.shape({
    setRouteLeaveHook: PropTypes.func.isRequired,
  }).isRequired,
};

ConfirmLeaveDialog.defaultProps = {
  question: 'Are you sure?',
};

export default withRouter(ConfirmLeaveDialog);
