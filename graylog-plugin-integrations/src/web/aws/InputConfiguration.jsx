import PropTypes from 'prop-types';

import history from 'util/History';
import Routes from 'common/Routes.js';

const InputConfiguration = ({ url }) => {
  history.push(url);

  return null;
};

InputConfiguration.propTypes = {
  url: PropTypes.string,
};

InputConfiguration.defaultProps = {
  url: Routes.INTEGRATIONS.AWS.CLOUDWATCH.index,
};

export default InputConfiguration;
