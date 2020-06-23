// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';

import FieldTypesContext from './FieldTypesContext';

const DefaultFieldTypesProvider = ({ children }: { children: React.Node }) => {
  const fieldTypes = useStore(FieldTypesStore);
  return (
    fieldTypes
      ? (
        <FieldTypesContext.Provider value={fieldTypes}>
          {children}
        </FieldTypesContext.Provider>
      )
      : children
  );
};

DefaultFieldTypesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default DefaultFieldTypesProvider;
