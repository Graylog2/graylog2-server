/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';

import { Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { useStore } from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import { LookupTablesActions, LookupTablesStore } from 'stores/lookup-tables/LookupTablesStore';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { ALL_MESSAGES_TIMERANGE } from 'views/Constants';

import LookupTableFieldValueProviderForm from './LookupTableFieldValueProviderForm';

const LOOKUP_PERMISSIONS = [
  'lookuptables:read',
];

type Props = {
  config: {},
  validation: {},
  currentUser: {
    permissions: Array<string>,
  },
  onChange: () => void,
}

const LookupTableFieldValueProviderFormContainer = ({ currentUser, ...otherProps }: Props) => {
  const { data: fieldTypes } = useFieldTypes([], ALL_MESSAGES_TIMERANGE);
  const lookupTables = useStore(LookupTablesStore);

  useEffect(() => {
    if (!isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS)) {
      return;
    }

    LookupTablesActions.searchPaginated(1, 0, undefined, false);
  }, [currentUser.permissions]);

  if (!isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS)) {
    return (
      <Row>
        <Col md={6} lg={5}>
          <p>No Lookup Tables found.</p>
        </Col>
      </Row>
    );
  }

  const isLoading = !fieldTypes || !lookupTables.tables;

  if (isLoading) {
    return <Spinner text="Loading Field Provider information..." />;
  }

  return (
    <LookupTableFieldValueProviderForm allFieldTypes={fieldTypes}
                                       lookupTables={lookupTables.tables}
                                       {...otherProps} />
  );
};

LookupTableFieldValueProviderFormContainer.propTypes = {
  config: PropTypes.object.isRequired,
  validation: PropTypes.object.isRequired,
  currentUser: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default LookupTableFieldValueProviderFormContainer;
