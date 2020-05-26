// @flow strict
import PropTypes from 'prop-types';
import type { ViewType } from 'views/logic/views/View';

type Props = {
  type: ViewType,
  capitalize?: boolean,
};

const capitalizeLabel = (label) => label[0].toUpperCase() + label.slice(1);

const ViewTypeLabel = ({ type, capitalize }: Props) => {
  const typeLabel = type.toLowerCase();
  return capitalize ? capitalizeLabel(typeLabel) : typeLabel;
};

ViewTypeLabel.propTypes = {
  type: PropTypes.string.isRequired,
  capitalize: PropTypes.bool,
};

ViewTypeLabel.defaultProps = {
  capitalize: false,
};

export default ViewTypeLabel;
