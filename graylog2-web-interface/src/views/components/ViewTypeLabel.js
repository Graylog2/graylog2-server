// @flow strict
import PropTypes from 'prop-types';
import StringUtils from 'util/StringUtils';
import type { ViewType } from 'views/logic/views/View';

type Props = {
  type: ViewType,
  capitalize?: boolean,
};

const ViewTypeLabel = ({ type, capitalize }: Props) => {
  const typeLabel = type.toLowerCase();
  return capitalize ? StringUtils.capitalizeFirstLetter(typeLabel) : typeLabel;
};

ViewTypeLabel.propTypes = {
  type: PropTypes.string.isRequired,
  capitalize: PropTypes.bool,
};

ViewTypeLabel.defaultProps = {
  capitalize: false,
};

export default ViewTypeLabel;
