import React from 'react';
import PropTypes from 'prop-types';

import { useFieldList } from './FieldListContext';

const FieldsByLink = ({ mode, text, title }) => {
  const { showFieldsBy, setShowFieldsBy } = useFieldList();
  const isCurrentShowFieldsBy = showFieldsBy === mode;

  return (
    // eslint-disable-next-line jsx-a11y/anchor-is-valid,jsx-a11y/click-events-have-key-events
    <a onClick={() => setShowFieldsBy(mode)}
       role="button"
       tabIndex={0}
       title={title}
       style={{ fontWeight: isCurrentShowFieldsBy ? 'bold' : 'normal' }}>
      {text}
    </a>
  );
};

FieldsByLink.propTypes = {
  mode: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
};

export default FieldsByLink;
