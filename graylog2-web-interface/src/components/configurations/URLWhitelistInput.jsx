import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { Input } from 'components/bootstrap';
import URLWhitelistFormModal from 'components/configurations/URLWhitelistFormModal';


const URLWhitelistInput = ({ label, handleFormEvent, validationMessage, validationState, url }) => {
  const triggerInput = (urlInput) => {
    const input = document.getElementById(urlInput.props.name);
    const tracker = input._valueTracker;
    const event = new Event('change', { bubbles: true });
    event.simulated = true;
    if (tracker) {
      tracker.setValue('');
    }
    input.dispatchEvent(event);
  };

  const ref = useRef();

  const onUpdate = () => {
    triggerInput(ref.current);
  };

  const addButton = validationState === 'error' ? <URLWhitelistFormModal newUrlEntry={url} onUpdate={onUpdate} /> : '';
  return (
    <Input type="text"
           id="url"
           name="url"
           label={label}
           ref={ref}
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
