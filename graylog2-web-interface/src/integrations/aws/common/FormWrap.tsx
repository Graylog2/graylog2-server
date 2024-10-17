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
import React, { useEffect, useRef, useState } from 'react';
import styled, { createGlobalStyle, css } from 'styled-components';

import { Button, Panel } from 'components/bootstrap';
import Icon from 'components/common/Icon';

type ErrorMessageProps = {
  fullMessage: string;
  niceMessage?: string | React.ReactNode;
};

type FormWrapProps = {
  buttonContent?: string | React.ReactNode;
  children: any;
  disabled?: boolean;
  error?: {
    full_message: string;
    nice_message?: string | React.ReactNode;
  };
  description?: string | React.ReactNode;
  loading?: boolean;
  onSubmit?: (...args: any[]) => void;
  title?: string | React.ReactNode;
  className?: string;
};

const FormWrap = ({
  buttonContent = 'Submit',
  children,
  className,
  disabled = false,
  description = null,
  error = null,
  loading = false,
  onSubmit = () => {},
  title = null,
}: FormWrapProps) => {
  const formRef = useRef();
  const [disabledButton, setDisabledButton] = useState(disabled);

  const prevent = (event) => {
    event.preventDefault();

    return false;
  };

  useEffect(() => {
    setDisabledButton(loading || disabled);
  }, [loading, disabled]);

  return (
    <form onSubmit={prevent}
          autoComplete="off"
          noValidate
          className={className}
          ref={formRef}>

      {title && ((typeof (title) === 'string') ? <h2>{title}</h2> : title)}
      {description && ((typeof (description) === 'string') ? <p>{description}</p> : description)}

      {error && error.full_message && (
        <ErrorMessage fullMessage={error.full_message}
                      niceMessage={error.nice_message} />
      )}

      {children}

      <Button type="button"
              onClick={disabledButton ? null : onSubmit}
              bsStyle="primary"
              disabled={disabledButton}>
        {loading ? 'Loading...' : buttonContent}
      </Button>
    </form>
  );
};

const ErrorOutputStyle = createGlobalStyle`
  /* NOTE: This is to remove Bootstrap styles from the anchor element I can't override in Panel.Header */
  form {
    .panel.panel-danger {
      .panel-heading > a {
        font-size: 14px;
        text-decoration: none;
        color: #ad0707;

        &:hover {
          text-decoration: none;
        }
      }
    }
  }
`;

const ErrorOutput = styled.span`
  display: block;
`;

const ErrorToggleInfo = styled.button`
  border: 0;
  background: none;
  color: #1f1f1f;
  font-size: 11px;
  text-transform: uppercase;
  margin: 12px 0 0;
  padding: 0;
`;

const MoreIcon = styled(Icon)<{ expanded: boolean }>(({ expanded }) => css`
  transform: rotate(${expanded ? '90deg' : '0deg'});
  transition: 150ms transform ease-in-out;
`);

export const ErrorMessage = ({
  fullMessage,
  niceMessage = null,
}: ErrorMessageProps) => {
  const [expanded, toggleExpanded] = useState(false);

  const Header = (
    <>
      <ErrorOutputStyle />
      <ErrorOutput>{niceMessage || fullMessage}</ErrorOutput>
      {niceMessage
        && (
          <ErrorToggleInfo onClick={() => toggleExpanded(!expanded)}>
            More Info <MoreIcon name="chevron_right" expanded={expanded} />
          </ErrorToggleInfo>
        )}
    </>
  );

  if (!niceMessage) {
    return <Panel header={Header} bsStyle="danger" />;
  }

  return (
    <Panel header={Header}
           bsStyle="danger"
           collapsible
           expanded={expanded}>
      <strong>Additional Information: </strong>{fullMessage}
    </Panel>
  );
};

export default FormWrap;
