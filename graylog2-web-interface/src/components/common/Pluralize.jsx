import PropTypes from 'prop-types';
import React from 'react';

import StringUtils from 'util/StringUtils';

/**
 * Component that will render a singular or plural text depending on a given value.
 */
class Pluralize extends React.Component {
  static propTypes = {
    /** Singular form of the word. */
    singular: PropTypes.string.isRequired,
    /** Plural form of the word. */
    plural: PropTypes.string.isRequired,
    /** Value to use to decide which form will be rendered. */
    value: PropTypes.oneOfType([
      PropTypes.number,
      PropTypes.string,
    ]).isRequired,
  };

  render() {
    return <span>{StringUtils.pluralize(this.props.value, this.props.singular, this.props.plural)}</span>;
  }
}

export default Pluralize;
