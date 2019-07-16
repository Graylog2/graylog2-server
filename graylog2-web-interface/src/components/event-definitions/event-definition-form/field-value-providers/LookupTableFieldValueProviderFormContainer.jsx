import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import LookupTableFieldValueProviderForm from './LookupTableFieldValueProviderForm';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

class LookupTableFieldValueProviderFormContainer extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    eventFields: PropTypes.object.isRequired,
    fieldTypes: PropTypes.object.isRequired,
    lookupTables: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    LookupTablesActions.searchPaginated(1, 0, undefined, false);
  }

  render() {
    const { lookupTables, fieldTypes, ...otherProps } = this.props;
    const isLoading = typeof fieldTypes.all !== 'object' || !lookupTables.tables;
    if (isLoading) {
      return <Spinner text="Loading Field Provider information..." />;
    }

    return (
      <LookupTableFieldValueProviderForm allFieldTypes={fieldTypes.all.toJS()}
                                         lookupTables={lookupTables.tables}
                                         {...otherProps} />
    );
  }
}

export default connect(LookupTableFieldValueProviderFormContainer, {
  fieldTypes: FieldTypesStore,
  lookupTables: LookupTablesStore,
});
