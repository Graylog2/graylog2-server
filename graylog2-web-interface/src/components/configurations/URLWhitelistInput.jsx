import React, { useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Input } from 'components/bootstrap';
import URLWhitelistFormModal from 'components/configurations/URLWhitelistFormModal';
import StoreProvider from 'injection/StoreProvider';
import URLUtils from 'util/URLUtils';
import UIUtils from 'util/UIUtils';

const ToolsStore = StoreProvider.getStore('Tools');

const URLWhitelistInput = ({ label, handleFormEvent, validationMessage, validationState, url, labelClassName, wrapperClassName, formType }) => {
  const [isWhitelisted, setIsWhitelisted] = useState(false);
  const [currentValidationState, setCurrentValidationState] = useState(validationState);
  const [ownValidationMessage, setOwnValidationMessage] = useState(validationMessage);

  const suggestRegexWhitelistUrl = (typedUrl, type) => {
    // eslint-disable-next-line no-template-curly-in-string
    const keyWildcard = '${key}';
    return type && type === 'regex' && URLUtils.isValidURL(typedUrl) ? ToolsStore.urlWhiteListGenerateRegex(typedUrl, keyWildcard) : typedUrl;
  };

  const [suggestedUrl, setSuggestedUrl] = useState(url);
  const isError = () => currentValidationState === 'error';
  const ref = useRef();

  const handleCheckIsWhitelisted = () => {
    const promise = ToolsStore.urlWhiteListCheck(url);
    promise.then((result) => {
      if (!result.is_whitelisted && validationState === null) {
        setCurrentValidationState('error');
        setOwnValidationMessage(`URL ${suggestedUrl} is not whitelisted or not valid URL.`);
      } else {
        setOwnValidationMessage(validationMessage);
        setCurrentValidationState(validationState);
        setIsWhitelisted(result.is_whitelisted);
      }
    });
  };

  const checkIsWhitelisted = () => {
    if (url) {
      const suggestion = suggestRegexWhitelistUrl(url, formType);
      if (typeof suggestion === 'object') {
        suggestion.then((result) => {
          setSuggestedUrl(result.regex);
          handleCheckIsWhitelisted();
        });
      } else {
        setSuggestedUrl(url);
        handleCheckIsWhitelisted();
      }
    }
  };


  const onUpdate = () => {
    UIUtils.triggerInput(ref.current);
    checkIsWhitelisted();
  };

  const handleChange = (event) => {
    handleFormEvent(event);
  };

  useEffect(() => {
    checkIsWhitelisted();
  }, [url, validationState]);

  const addButton = isError() && !isWhitelisted ? <URLWhitelistFormModal newUrlEntry={suggestedUrl} onUpdate={onUpdate} formType={formType} /> : '';
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
  formType: PropTypes.string,
};

URLWhitelistInput.defaultProps = {
  url: '',
  validationState: '',
  validationMessage: '',
  labelClassName: '',
  wrapperClassName: '',
  formType: '',
};

export default URLWhitelistInput;
