import React, { useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Input } from 'components/bootstrap';
import URLWhitelistFormModal from 'components/configurations/URLWhitelistFormModal';
import StoreProvider from 'injection/StoreProvider';

const ToolsStore = StoreProvider.getStore('Tools');

const URLWhitelistInput = ({ label, handleFormEvent, validationMessage, validationState, url, labelClassName, wrapperClassName }) => {
  const [isWhitelisted, setIsWhitelisted] = useState(false);
  const [currentValidationState, setCurrentValidationState] = useState(validationState);
  const [ownValidationMessage, setOwnValidationMessage] = useState(validationMessage);
  const isError = () => currentValidationState === 'error';

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

  const checkIsWhitelisted = () => {
    if (url) {
      const promise = ToolsStore.urlWhiteListCheck(url);
      promise.then((result) => {
        if (!result.is_whitelisted && validationState === null) {
          setCurrentValidationState('error');
          setOwnValidationMessage(`URL ${url} is not whitelisted.`);
        } else {
          setOwnValidationMessage(validationMessage);
          setCurrentValidationState(validationState);
        }
        setIsWhitelisted(result.is_whitelisted);
      });
    }
  };

  const onUpdate = () => {
    triggerInput(ref.current);
    checkIsWhitelisted();
  };

  const handleChange = (event) => {
    handleFormEvent(event);
  };

  useEffect(() => {
    checkIsWhitelisted();
  }, [url, validationState]);

  const addButton = isError() && !isWhitelisted ? <URLWhitelistFormModal newUrlEntry={url} onUpdate={onUpdate} /> : '';
  return (
    <Input type="text"
           id="url"
           name="url"
           label={label}
           ref={ref}
           autoFocus
           required
           onChange={handleChange}
           help={[ownValidationMessage, addButton]}
           bsStyle={currentValidationState}
           value={url}
           labelClassName={labelClassName}
           wrapperClassName={wrapperClassName} />
  );
};

URLWhitelistInput.propTypes = {
  label: PropTypes.string.isRequired,
  handleFormEvent: PropTypes.func.isRequired,
  validationState: PropTypes.string,
  validationMessage: PropTypes.oneOfType([
    PropTypes.object,
    PropTypes.string,
  ]),
  url: PropTypes.string,
  labelClassName: PropTypes.string,
  wrapperClassName: PropTypes.string,
};

URLWhitelistInput.defaultProps = {
  url: '',
  validationState: '',
  validationMessage: '',
  labelClassName: '',
  wrapperClassName: '',
};

export default URLWhitelistInput;
