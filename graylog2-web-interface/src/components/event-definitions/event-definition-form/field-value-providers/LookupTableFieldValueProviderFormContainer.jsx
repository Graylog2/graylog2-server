import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import LookupTableFieldValueProviderForm from './LookupTableFieldValueProviderForm';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

class LookupTableFieldValueProviderFormContainer extends React.Component {
  static propTypes = {
    lookupTables: PropTypes.object.isRequired,
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    LookupTablesActions.searchPaginated(1, 0, undefined, false);
  }

  render() {
    const { lookupTables, ...otherProps } = this.props;
    if (!lookupTables.tables) {
      return <Spinner text="Loading Lookup Tables information..." />;
    }

    return <LookupTableFieldValueProviderForm lookupTables={lookupTables.tables} {...otherProps} />;
  }
}

export default connect(LookupTableFieldValueProviderFormContainer, { lookupTables: LookupTablesStore });
