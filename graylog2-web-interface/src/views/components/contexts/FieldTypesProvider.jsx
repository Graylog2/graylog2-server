// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import FieldTypesContext from './FieldTypesContext';

const FieldTypesProvider = ({ children }: { children: React.Node }) => {
  const fieldTypes = useStore(FieldTypesStore);
  return (
    <FieldTypesContext.Provider value={fieldTypes}>
      {children}
    </FieldTypesContext.Provider>
  );
};

FieldTypesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default FieldTypesProvider;
