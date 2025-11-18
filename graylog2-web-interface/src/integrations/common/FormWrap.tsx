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
import * as React from 'react';
import { useEffect, useRef, useState } from 'react';
import styled, { css, createGlobalStyle } from 'styled-components';

import { Icon } from 'components/common';
import { Button, Panel } from 'components/bootstrap';

import type { ErrorMessageType } from './utils/types';

const ErrorOutputStyle = createGlobalStyle`
  /* NOTE: This is to remove Bootstrap styles from the anchor element I can't override in Panel.Header */
  form {
    .panel.panel-danger {
      .panel-heading > a {
        font-size: 14px;
        text-decoration: none;
        color: #AD0707;

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

const ErrorToggleInfo = styled.button(
  ({ theme }) => css`
    border: 0;
    background: none;
    color: ${theme.colors.gray[10]};
    font-size: ${theme.fonts.size.small};
    text-transform: uppercase;
    margin: 12px 0 0;
    padding: 0;
    &:hover {
      text-decoration: underline;
    }
  `,
);

export const ErrorMessage = ({ full_message, nice_message = null }: ErrorMessageType) => {
  const [expanded, toggleExpanded] = useState<boolean>(false);

  const Header = (
    <>
      <ErrorOutputStyle />
      <ErrorOutput>{nice_message || full_message}</ErrorOutput>
      {nice_message && (
        <ErrorToggleInfo onClick={() => toggleExpanded(!expanded)}>
          More Info <Icon name={expanded ? 'keyboard_arrow_down' : 'chevron_right'} />
        </ErrorToggleInfo>
      )}
    </>
  );

  if (!nice_message) {
    return <Panel header={Header} bsStyle="danger" />;
  }

  return (
    <Panel header={Header} bsStyle="danger" collapsible expanded={expanded}>
      <strong>Additional Information: </strong>
      {full_message}
    </Panel>
  );
};

type Props = {
  buttonContent?: React.ReactNode;
  className?: string;
  disabled?: boolean;
  description?: React.ReactNode;
  error?: ErrorMessageType;
  loading?: boolean;
  onSubmit: (e: React.FormEvent) => void;
  title?: React.ReactNode | string;
};

const FormWrap = ({
  buttonContent = 'Submit',
  children = undefined,
  className = undefined,
  disabled = false,
  description = null,
  error = null,
  loading = false,
  onSubmit,
  title = null,
}: React.PropsWithChildren<Props>) => {
  const formRef = useRef<HTMLFormElement>(null);
  const [disabledButton, setDisabledButton] = useState<boolean>(disabled);

  const prevent: (event: React.FormEvent<HTMLFormElement>) => boolean = (event) => {
    event.preventDefault();

    return false;
  };

  useEffect(() => {
    setDisabledButton(loading || disabled);
  }, [loading, disabled]);

  return (
    <form onSubmit={prevent} autoComplete="off" noValidate className={className} ref={formRef}>
      {title && (typeof title === 'string' ? <h2>{title}</h2> : title)}
      {description && (typeof description === 'string' ? <p>{description}</p> : description)}

      {error && error.full_message && (
        <ErrorMessage full_message={error.full_message} nice_message={error.nice_message} />
      )}

      {children}

      <Button type="button" onClick={disabledButton ? null : onSubmit} bsStyle="primary" disabled={disabledButton}>
        {loading ? 'Loading...' : buttonContent}
      </Button>
    </form>
  );
};

export default FormWrap;
