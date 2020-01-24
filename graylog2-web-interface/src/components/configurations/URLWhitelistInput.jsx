import React from 'react';
import PropTypes from 'prop-types';
import { Input } from 'components/bootstrap';
import URLWhitelistFormModal from 'components/configurations/URLWhitelistFormModal';

const URLWhitelistInput = ({ label, handleFormEvent, validationMessage, validationState, url }) => {
  const addButton = validationState === 'error' ? <URLWhitelistFormModal newUrlEntry={url} /> : '';
  return (
    <Input type="text"
           id="url"
           name="url"
           label={label}
           autoFocus
           required
           onChange={handleFormEvent}
           help={[validationMessage, addButton]}
           bsStyle={validationState}
           value={url}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
  );
};

URLWhitelistInput.propTypes = {
  label: PropTypes.string.isRequired,
  handleFormEvent: PropTypes.func.isRequired,
  validationState: PropTypes.string.isRequired,
  validationMessage: PropTypes.string.isRequired,
  url: PropTypes.string,

};

URLWhitelistInput.defaultProps = {
  url: '',
};

export default URLWhitelistInput;
