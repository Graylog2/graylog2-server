import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';

import FilterAggregationForm from './FilterAggregationForm';
import withStreams from './withStreams';

// We currently don't support creating Events from these Streams, since they also contain Events
// and it's not possible to access custom Fields defined in them.
const HIDDEN_STREAMS = [
  '000000000000000000000002',
  '000000000000000000000003',
];

class FilterAggregationFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']).isRequired,
    validation: PropTypes.object.isRequired,
    eventDefinition: PropTypes.object.isRequired,
    fieldTypes: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    currentUser: PropTypes.object.isRequired, // Prop is passed down to pluggable entities
  };

  render() {
    const { fieldTypes, ...otherProps } = this.props;
    const isLoading = typeof fieldTypes.all !== 'object';

    if (isLoading) {
      return <Spinner text="Loading Filter & Aggregation Information..." />;
    }

    return <FilterAggregationForm allFieldTypes={fieldTypes.all.toJS()} {...otherProps} />;
  }
}

export default connect(withStreams(FilterAggregationFormContainer, HIDDEN_STREAMS), {
  fieldTypes: FieldTypesStore,
});
