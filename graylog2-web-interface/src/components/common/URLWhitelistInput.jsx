// @flow strict
import React, { useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Input } from 'components/bootstrap';
import URLWhitelistFormModal from 'components/configurations/URLWhitelistFormModal';
import StoreProvider from 'injection/StoreProvider';
import URLUtils from 'util/URLUtils';
import FormsUtils from 'util/FormsUtils';

const ToolsStore = StoreProvider.getStore('Tools');
type Props = {
  label: string,
  onChange: (event: SyntheticInputEvent<EventTarget>) => void,
  validationMessage: string,
  validationState: string,
  url: string,
  labelClassName: string,
  wrapperClassName: string,
  urlType: string
}
const URLWhitelistInput = ({ label, onChange, validationMessage, validationState, url, labelClassName, wrapperClassName, urlType }: Props) => {
  const [isWhitelisted, setIsWhitelisted] = useState(false);
  const [currentValidationState, setCurrentValidationState] = useState(validationState);
  const [ownValidationMessage, setOwnValidationMessage] = useState(validationMessage);

  const suggestRegexWhitelistUrl = (typedUrl: string, type: string): string | Promise<any> => {
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
        setOwnValidationMessage(`URL ${url} is not whitelisted or not valid URL.`);
      } else {
        setOwnValidationMessage(validationMessage);
        setCurrentValidationState(validationState);
      }
      setIsWhitelisted(result.is_whitelisted);
    });
  };

  const checkIsWhitelisted = () => {
    if (url) {
      const suggestion = suggestRegexWhitelistUrl(url, urlType);
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
    FormsUtils.triggerInput(ref.current);
    checkIsWhitelisted();
  };

  useEffect(() => {
    checkIsWhitelisted();
  }, [url, validationState]);


  const addButton = isError() && !isWhitelisted ? <URLWhitelistFormModal newUrlEntry={suggestedUrl} onUpdate={onUpdate} urlType={urlType} /> : '';
  const helpMessage = <>{validationState === null ? ownValidationMessage : validationMessage} {addButton}</>;
  return (
    <Input type="text"
           id="url"
           name="url"
           label={label}
           ref={ref}
           autoFocus
           required
           onChange={onChange}
           help={helpMessage}
           bsStyle={currentValidationState}
           value={url}
           labelClassName={labelClassName}
           wrapperClassName={wrapperClassName} />
  );
};

URLWhitelistInput.propTypes = {
  label: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  validationState: PropTypes.string,
  validationMessage: PropTypes.oneOfType([
    PropTypes.object,
    PropTypes.string,
  ]),
  url: PropTypes.string,
  labelClassName: PropTypes.string,
  wrapperClassName: PropTypes.string,
  urlType: PropTypes.string,
};

URLWhitelistInput.defaultProps = {
  url: '',
  validationState: '',
  validationMessage: '',
  labelClassName: '',
  wrapperClassName: '',
  urlType: '',
};

export default URLWhitelistInput;
