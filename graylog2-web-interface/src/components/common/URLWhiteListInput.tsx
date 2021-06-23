/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useRef, useState, useEffect, SyntheticEvent } from 'react';
import PropTypes from 'prop-types';

import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
import { isValidURL } from 'util/URLUtils';
import * as FormsUtils from 'util/FormsUtils';
// Explicit import to fix eslint import/no-cycle
import URLWhiteListFormModal from 'components/common/URLWhiteListFormModal';

const ToolsStore = StoreProvider.getStore('Tools');

type Props = {
  label: string,
  onChange: (event: SyntheticEvent<EventTarget>) => void,
  validationMessage: string,
  validationState: string,
  url: string,
  labelClassName: string,
  wrapperClassName: string,
  urlType: string,
};

const URLWhiteListInput = ({ label, onChange, validationMessage, validationState, url, labelClassName, wrapperClassName, urlType }: Props) => {
  const [isWhitelisted, setIsWhitelisted] = useState(false);
  const [currentValidationState, setCurrentValidationState] = useState(validationState);
  const [ownValidationMessage, setOwnValidationMessage] = useState(validationMessage);

  const suggestRegexWhitelistUrl = (typedUrl: string, type: string): string | Promise<any> => {
    // eslint-disable-next-line no-template-curly-in-string
    const keyWildcard = '${key}';

    return type && type === 'regex' && isValidURL(typedUrl) ? ToolsStore.urlWhiteListGenerateRegex(typedUrl, keyWildcard) : typedUrl;
  };

  const [suggestedUrl, setSuggestedUrl] = useState(url);
  const isWhitelistError = () => currentValidationState === 'error' && isValidURL(url);
  const ref = useRef();

  const checkIsWhitelisted = () => {
    if (url) {
      const promise = ToolsStore.urlWhiteListCheck(url);

      promise.then((result) => {
        if (!result.is_whitelisted && validationState === null) {
          setCurrentValidationState('error');
          const message = isValidURL(url) ? `URL ${url} is not whitelisted` : `URL ${url} is not a valid URL.`;

          setOwnValidationMessage(message);
        } else {
          setOwnValidationMessage(validationMessage);
          setCurrentValidationState(validationState);
        }

        setIsWhitelisted(result.is_whitelisted);
      });
    }
  };

  const onUpdate = () => {
    FormsUtils.triggerInput(ref.current);
    checkIsWhitelisted();
  };

  useEffect(() => {
    const checkSuggestion = () => {
      if (url) {
        const suggestion = suggestRegexWhitelistUrl(url, urlType);

        if (typeof suggestion === 'object') {
          suggestion.then((result) => {
            setSuggestedUrl(result.regex);
          });
        } else {
          setSuggestedUrl(url);
        }
      }
    };

    const timer = setTimeout(() => checkSuggestion(), 250);

    return () => clearTimeout(timer);
  }, [url]);

  useEffect(() => {
    const timer = setTimeout(() => checkIsWhitelisted(), 250);

    return () => clearTimeout(timer);
  }, [url, validationState]);

  const addButton = isWhitelistError() && !isWhitelisted ? <URLWhiteListFormModal newUrlEntry={suggestedUrl} onUpdate={onUpdate} urlType={urlType} /> : '';
  const helpMessage = <>{validationState === null ? ownValidationMessage : validationMessage} {addButton}</>;
  const bsStyle = currentValidationState === '' ? null : currentValidationState;

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
           bsStyle={bsStyle}
           value={url}
           labelClassName={labelClassName}
           wrapperClassName={wrapperClassName} />
  );
};

URLWhiteListInput.propTypes = {
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
  urlType: PropTypes.oneOf(['regex', 'literal']),
};

URLWhiteListInput.defaultProps = {
  url: '',
  validationState: '',
  validationMessage: '',
  labelClassName: '',
  wrapperClassName: '',
  urlType: 'literal',
};

export default URLWhiteListInput;
