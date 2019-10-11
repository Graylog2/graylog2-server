import React from 'react';
import PropTypes from 'prop-types';

import Filter from './Filter';
import FieldListProvider from './FieldListContext';
import FieldsByLink from './FieldsByLink';
import FieldListWrap from './FieldListWrap';

const FieldList = ({ fields, allFields, viewMetadata }) => {
  return (
    <FieldListProvider>
      <Filter />

      <div>
        List fields of{' '}
        <FieldsByLink mode="current"
                      text="current streams"
                      title="This shows fields which are (prospectively) included in the streams you have selected." />,{' '}

        <FieldsByLink mode="all"
                      text="all"
                      title="This shows all fields, but no reserved (gl2_*) fields." /> or{' '}

        <FieldsByLink mode="allreserved"
                      text="all including reserved"
                      title="This shows all fields, including reserved (gl2_*) fields." />
      </div>

      <hr />

      <FieldListWrap fields={fields} allFields={allFields} viewMetadata={viewMetadata} />
    </FieldListProvider>
  );
};

FieldList.propTypes = {
  allFields: PropTypes.object.isRequired,
  fields: PropTypes.object.isRequired,
  viewMetadata: PropTypes.shape({
    id: PropTypes.number,
    activeQuery: PropTypes.string,
  }).isRequired,
};

export default FieldList;
