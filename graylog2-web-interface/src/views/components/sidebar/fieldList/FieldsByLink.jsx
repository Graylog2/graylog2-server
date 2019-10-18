import React from 'react';
import PropTypes from 'prop-types';

import { useFieldList } from './FieldListContext';

const FieldsByLink = ({ className, mode, text, title }) => {
  const { showFieldsBy, setShowFieldsBy } = useFieldList();
  const isCurrentShowFieldsBy = showFieldsBy === mode;

  return (
    // eslint-disable-next-line jsx-a11y/anchor-is-valid,jsx-a11y/click-events-have-key-events
    <a onClick={() => setShowFieldsBy(mode)}
       role="button"
       tabIndex={0}
       title={title}
       className={className}
       style={{ fontWeight: isCurrentShowFieldsBy ? 'bold' : 'normal' }}>
      {text}
    </a>
  );
};

FieldsByLink.propTypes = {
  mode: PropTypes.string.isRequired,
  className: PropTypes.string,
  text: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
};

FieldsByLink.defaultProps = {
  className: undefined,
};

export default FieldsByLink;
