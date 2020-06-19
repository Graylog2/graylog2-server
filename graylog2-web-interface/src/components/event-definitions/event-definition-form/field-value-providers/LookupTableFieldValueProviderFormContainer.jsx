import React from 'react';
import PropTypes from 'prop-types';

import { Col, Row } from 'components/graylog';
import { Spinner } from 'components/common';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import PermissionsMixin from 'util/PermissionsMixin';

import LookupTableFieldValueProviderForm from './LookupTableFieldValueProviderForm';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

const LOOKUP_PERMISSIONS = [
  'lookuptables:read',
];

class LookupTableFieldValueProviderFormContainer extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    fieldTypes: PropTypes.object.isRequired,
    lookupTables: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    const { currentUser } = this.props;
    if (!PermissionsMixin.isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS)) {
      return;
    }

    LookupTablesActions.searchPaginated(1, 0, undefined, false);
  }

  render() {
    const { lookupTables, fieldTypes, currentUser, ...otherProps } = this.props;

    if (!PermissionsMixin.isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS)) {
      return (
        <Row>
          <Col md={6} lg={5}>
            <p>No Lookup Tables found.</p>
          </Col>
        </Row>
      );
    }

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
