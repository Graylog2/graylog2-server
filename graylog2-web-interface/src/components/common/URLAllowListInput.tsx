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
import type { SyntheticEvent } from 'react';
import React, { useCallback, useRef, useState, useEffect } from 'react';

import { Input } from 'components/bootstrap';
import { isValidURL } from 'util/URLUtils';
// Explicit import to fix eslint import/no-cycle
import URLAllowListFormModal from 'components/common/URLAllowListFormModal';
import ToolsStore from 'stores/tools/ToolsStore';
import { triggerInput } from 'util/FormsUtils';
import type { ValidationState } from 'components/common/types';

type Props = {
  label: string;
  onChange: (event: SyntheticEvent<EventTarget>) => void;
  validationMessage?: string;
  validationState?: ValidationState;
  url?: string;
  onValidationChange?: (validationState: string) => void;
  labelClassName?: string;
  wrapperClassName?: string;
  urlType?: React.ComponentProps<typeof URLAllowListFormModal>['urlType'];
  autofocus?: boolean;
};

const URLAllowListInput = ({
  label,
  onChange,
  validationMessage = '',
  validationState = undefined,
  url = '',
  onValidationChange = () => {},
  labelClassName = '',
  wrapperClassName = '',
  urlType = 'literal',
  autofocus = true,
}: Props) => {
  const [isAllowlisted, setIsAllowlisted] = useState(false);
  const [currentValidationState, setCurrentValidationState] = useState(validationState);
  const [ownValidationMessage, setOwnValidationMessage] = useState(validationMessage);

  const suggestRegexAllowlistUrl = (typedUrl: string, type: string): string | Promise<any> => {
    // eslint-disable-next-line no-template-curly-in-string
    const keyWildcard = '${key}';

    return type && type === 'regex' && isValidURL(typedUrl)
      ? ToolsStore.urlAllowListGenerateRegex(typedUrl, keyWildcard)
      : typedUrl;
  };

  const [suggestedUrl, setSuggestedUrl] = useState(url);
  const isAllowlistError = () => currentValidationState === 'error' && isValidURL(url);
  const urlInputRef = useRef<Input>();

  const checkIsAllowlisted = useCallback(() => {
    if (url) {
      const promise = ToolsStore.urlAllowListCheck(url);

      promise.then((result) => {
        if (!result.is_allowlisted && validationState === null) {
          setCurrentValidationState('error');
          onValidationChange('error');
          const message = isValidURL(url) ? `URL ${url} is not allowlisted` : `URL ${url} is not a valid URL.`;

          setOwnValidationMessage(message);
        } else {
          setOwnValidationMessage(validationMessage);
          setCurrentValidationState(validationState);
          onValidationChange(validationState);
        }

        setIsAllowlisted(result.is_allowlisted);
      });
    }
  }, [url, validationMessage, validationState, onValidationChange]);

  const onUpdate = () => {
    triggerInput(urlInputRef.current.getInputDOMNode());
    checkIsAllowlisted();
  };

  useEffect(() => {
    const checkSuggestion = () => {
      if (url) {
        const suggestion = suggestRegexAllowlistUrl(url, urlType);

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
  }, [url, urlType]);

  useEffect(() => {
    const timer = setTimeout(() => checkIsAllowlisted(), 250);

    return () => clearTimeout(timer);
  }, [url, validationState, checkIsAllowlisted]);

  const addButton =
    isAllowlistError() && !isAllowlisted ? (
      <URLAllowListFormModal newUrlEntry={suggestedUrl} onUpdate={onUpdate} urlType={urlType} />
    ) : (
      ''
    );
  const helpMessage = (
    <>
      {validationState === null ? ownValidationMessage : validationMessage} {addButton}
    </>
  );

  return (
    <Input
      type="text"
      id="url"
      name="url"
      label={label}
      ref={urlInputRef}
      autoFocus={autofocus}
      required
      onChange={onChange}
      help={helpMessage}
      bsStyle={currentValidationState}
      value={url}
      labelClassName={labelClassName}
      wrapperClassName={wrapperClassName}
    />
  );
};

export default URLAllowListInput;
