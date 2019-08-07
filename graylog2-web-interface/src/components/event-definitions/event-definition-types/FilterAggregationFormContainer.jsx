import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';

import FilterAggregationForm from './FilterAggregationForm';

const { StreamsStore } = CombinedProvider.get('Streams');

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
  };

  state = {
    availableStreams: undefined,
  };

  componentDidMount() {
    StreamsStore.load((streams) => {
      const filteredStreams = streams.filter(s => !HIDDEN_STREAMS.includes(s.id));
      this.setState({ availableStreams: filteredStreams });
    });
  }

  render() {
    const { fieldTypes, ...otherProps } = this.props;
    const { availableStreams } = this.state;
    const isLoading = typeof fieldTypes.all !== 'object' || !availableStreams;

    if (isLoading) {
      return <Spinner text="Loading Filter & Aggregation Information..." />;
    }

    return (
      <FilterAggregationForm allFieldTypes={fieldTypes.all.toJS()}
                             streams={availableStreams}
                             {...otherProps} />
    );
  }
}

export default connect(FilterAggregationFormContainer, {
  fieldTypes: FieldTypesStore,
});
