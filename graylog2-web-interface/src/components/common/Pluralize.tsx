import React from 'react';

import StringUtils from 'util/StringUtils';

type PluralizeProps = {
  /** Singular form of the word. */
  singular: string;
  /** Plural form of the word. */
  plural: string;
  /** Value to use to decide which form will be rendered. */
  value: number | string;
};

/**
 * Component that will render a singular or plural text depending on a given value.
 */
class Pluralize extends React.Component<PluralizeProps, {
  [key: string]: any;
}> {
  render() {
    return <span>{StringUtils.pluralize(this.props.value, this.props.singular, this.props.plural)}</span>;
  }
}

export default Pluralize;
