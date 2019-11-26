import PropTypes from 'prop-types';

import { bsStyles } from '../variants/bsStyle';

const propTypes = {
  /* Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf([...bsStyles, 'link']),
};

const defaultProps = {
  bsStyle: 'default',
};

export { propTypes, defaultProps };
