import PropTypes from 'prop-types';

export const FieldList = PropTypes.arrayOf(
  PropTypes.shape({
    key: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
  }),
);
