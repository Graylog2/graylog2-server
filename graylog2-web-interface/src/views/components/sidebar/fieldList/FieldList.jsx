import React from 'react';
import PropTypes from 'prop-types';

import Filter from './Filter';
import FieldListProvider from './FieldListContext';
import FieldsToggle from './FieldsToggle';
import FieldListWrap from './FieldListWrap';

const FieldList = ({ fields, allFields, viewMetadata }) => {
  return (
    <FieldListProvider>
      <Filter />

      <FieldsToggle />

      <FieldListWrap fields={fields} allFields={allFields} viewMetadata={viewMetadata} />
    </FieldListProvider>
  );
};

FieldList.propTypes = {
  allFields: PropTypes.object.isRequired,
  fields: PropTypes.object.isRequired,
  viewMetadata: PropTypes.shape({
    id: PropTypes.string,
    activeQuery: PropTypes.string,
  }).isRequired,
};

export default FieldList;
