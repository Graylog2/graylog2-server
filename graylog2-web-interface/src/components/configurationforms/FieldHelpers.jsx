import React from 'react';

const FieldHelpers = {
  hasAttribute: (ary, attribute) => {
    return ary.indexOf(attribute) > -1;
  },
  optionalMarker: (field) => {
    return field.is_optional && field.type !== 'boolean' ? <span className="configuration-field-optional">(optional)</span> : null;
  },
};

/** @component */
export default FieldHelpers;
