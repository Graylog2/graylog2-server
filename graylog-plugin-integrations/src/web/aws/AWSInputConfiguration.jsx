import PropTypes from 'prop-types';

import history from 'util/History';
import Routes from 'aws/common/Routes.js';

const AWSInputConfiguration = ({ url }) => {
  history.push(url);

  return null;
};

AWSInputConfiguration.propTypes = {
  url: PropTypes.string,
};

AWSInputConfiguration.defaultProps = {
  url: Routes.INTEGRATIONS.AWS.CLOUDWATCH.index,
};

export default AWSInputConfiguration;
