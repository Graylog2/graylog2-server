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
import { useMemo, useState } from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';
import styled, { css } from 'styled-components';
import debounce from 'lodash/debounce';

import { DocumentTitle } from 'components/common';
import { qualifyUrl } from 'util/URLUtils';

const StyledSwaggerContainer = styled.div(
  ({ theme }) => css`
    /* General text colors */
    .swagger-ui {
      color: ${theme.colors.text.primary};
      font-family: ${theme.fonts.family.body};
    }

    /* Info section - title, description, version */
    .swagger-ui .info .title,
    .swagger-ui .info h1,
    .swagger-ui .info h2,
    .swagger-ui .info h3,
    .swagger-ui .info h4 {
      color: ${theme.colors.text.primary};
    }

    /* Version number badge (gray rounded pill) */
    .swagger-ui .info hgroup.main small:not(.version-stamp) {
      background-color: #7d8492;
      border-radius: 57px;
      padding: 2px 4px;
    }

    /* OAS version badge (green rounded pill) */
    .swagger-ui .info hgroup.main small.version-stamp {
      background-color: #89bf04;
      border-radius: 57px;
      padding: 2px 4px;
    }

    /* Version text inside badges */
    .swagger-ui .info hgroup.main pre.version {
      background-color: transparent;
      color: #fff;
      border: none;
      margin: 0;
      padding: 0;
    }

    .swagger-ui .info p,
    .swagger-ui .info li {
      color: ${theme.colors.text.primary};
    }

    /* Operation blocks - tag headers */
    .swagger-ui .opblock-tag {
      color: ${theme.colors.text.primary};
      border-bottom-color: ${theme.colors.gray[80]};
    }

    .swagger-ui .opblock-tag:hover {
      background-color: ${theme.colors.gray[90]};
    }

    /* Tag description text - needs better contrast in dark mode */
    .swagger-ui .opblock-tag small {
      color: ${theme.colors.text.secondary};
    }

    /* Expand/collapse arrows - make them visible */
    .swagger-ui .opblock-tag svg,
    .swagger-ui .opblock-tag .arrow,
    .swagger-ui .expand-operation svg {
      fill: ${theme.colors.text.primary};
    }

    .swagger-ui .opblock .opblock-summary-description {
      color: ${theme.colors.text.secondary};
    }

    .swagger-ui .opblock .opblock-summary-path {
      color: ${theme.colors.text.primary};
    }

    /* Operation expand/collapse button */
    .swagger-ui .opblock .opblock-summary-control svg {
      fill: ${theme.colors.text.primary};
    }

    /* Parameter and schema sections */
    .swagger-ui .opblock-description-wrapper p,
    .swagger-ui .opblock-external-docs-wrapper p,
    .swagger-ui table thead tr th,
    .swagger-ui table thead tr td,
    .swagger-ui .parameter__name,
    .swagger-ui .parameter__type,
    .swagger-ui .parameter__in {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .opblock-section-header {
      background-color: ${theme.colors.gray[90]};
    }

    .swagger-ui .opblock-section-header h4 {
      color: ${theme.colors.text.primary};
    }

    /* Try it out and cancel buttons need dark colors for contrast against light section header */
    .swagger-ui .opblock-section-header .try-out__btn,
    .swagger-ui .opblock-section-header .btn-group .cancel {
      color: #333;
      border-color: #333;
    }

    /* Tab headers (Parameters, etc.) */
    .swagger-ui .tab li {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .tab li button.tablinks {
      color: ${theme.colors.text.primary};
    }

    /* Schema/Model section */
    .swagger-ui .model-title {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .model {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .model .property,
    .swagger-ui .model .property.primitive {
      color: ${theme.colors.variant.info};
    }

    /* Response section */
    .swagger-ui .responses-inner h4,
    .swagger-ui .responses-inner h5 {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .response-col_status {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .response-col_description__inner p {
      color: ${theme.colors.text.primary};
    }

    /* Table styling */
    .swagger-ui table tbody tr td {
      color: ${theme.colors.text.primary};
    }

    /* Links */
    .swagger-ui a {
      color: ${theme.colors.global.link};
    }

    .swagger-ui a:hover {
      color: ${theme.colors.global.linkHover};
    }

    /* Code blocks */
    .swagger-ui .highlight-code {
      background-color: ${theme.colors.variant.lightest.default};
    }

    /* Markdown content */
    .swagger-ui .markdown p,
    .swagger-ui .markdown li {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui .markdown code {
      color: ${theme.colors.variant.darker.danger};
      background-color: ${theme.colors.variant.lightest.danger};
    }

    /* Filter input */
    .swagger-ui .filter-container input {
      color: ${theme.colors.input.color};
      background-color: ${theme.colors.input.background};
      border-color: ${theme.colors.input.border};
    }

    .swagger-ui .filter-container input::placeholder {
      color: ${theme.colors.input.placeholder};
    }

    /* Buttons - ensure visibility */
    .swagger-ui .btn {
      color: ${theme.colors.text.primary};
      border-color: ${theme.colors.text.primary};
    }

    .swagger-ui .btn:hover {
      color: ${theme.colors.text.primary};
    }

    /* Try it out button */
    .swagger-ui .try-out__btn {
      color: ${theme.colors.text.primary};
      border-color: ${theme.colors.text.primary};
      background-color: transparent;
    }

    /* Cancel button */
    .swagger-ui .btn-group .cancel {
      color: ${theme.colors.text.primary};
      border-color: ${theme.colors.text.primary};
    }

    .swagger-ui .btn-group .btn {
      color: ${theme.colors.text.primary};
    }

    /* Execute button */
    .swagger-ui .execute-wrapper .btn,
    .swagger-ui .btn.execute {
      color: #fff;
    }

    /* Hide schemes section (server selection and authorize button) */
    .swagger-ui .scheme-container {
      display: none;
    }

    .swagger-ui .servers-title,
    .swagger-ui .servers label {
      color: ${theme.colors.text.primary};
    }

    .swagger-ui select {
      color: ${theme.colors.input.color};
      background-color: ${theme.colors.input.background};
      border-color: ${theme.colors.input.border};
    }

    /* Copy to clipboard and download buttons */
    .swagger-ui .copy-to-clipboard,
    .swagger-ui .download-contents {
      background-color: ${theme.colors.gray[80]};
    }

    .swagger-ui .copy-to-clipboard button,
    .swagger-ui .download-contents button {
      color: ${theme.colors.text.primary};
    }

    /* JSON/Schema toggle */
    .swagger-ui .model-box-control,
    .swagger-ui .models-control {
      color: ${theme.colors.text.primary};
    }

    /* All SVG icons should use text color */
    .swagger-ui svg.arrow,
    .swagger-ui button svg {
      fill: ${theme.colors.text.primary};
    }
  `,
);

type JsonSchemaFormProps = {
  onChange: (newValue: string) => void;
  value: string;
};

type RequestBodyEditorProps = {
  onChange: (newValue: string) => void;
};

const FilterContainer = ({ specSelectors, layoutSelectors, getComponent, layoutActions }) => {
  const Col = useMemo(() => getComponent('Col'), [getComponent]);
  const [filterValue, setFilterValue] = useState<string>();

  const isLoading = specSelectors.loadingStatus() === 'loading';
  const isFailed = specSelectors.loadingStatus() === 'failed';
  const filter = layoutSelectors.currentFilter();

  const debouncedUpdateFilter = useMemo(
    () => debounce(layoutActions?.updateFilter, 1000),
    [layoutActions?.updateFilter],
  );

  const onFilterChange = (e) => {
    const value = e?.target?.value;
    setFilterValue(value);
    debouncedUpdateFilter(value);
  };

  const classNames = ['operation-filter-input'];
  if (isFailed) classNames.push('failed');
  if (isLoading) classNames.push('loading');

  return (
    <div>
      {filter === false ? null : (
        <div className="filter-container">
          {/* eslint-disable-next-line react-hooks/static-components */}
          <Col className="filter wrapper" mobile={12}>
            <input
              className={classNames.join(' ')}
              placeholder="Filter by tag"
              type="text"
              onChange={onFilterChange}
              value={filterValue}
              disabled={isLoading}
            />
          </Col>
        </div>
      )}
    </div>
  );
};
const DebounceInputsPlugin = () => ({
  wrapComponents: {
    JsonSchemaForm:
      (Original: React.FC<JsonSchemaFormProps>) =>
      ({ onChange, value, ...props }: JsonSchemaFormProps) => {
        // eslint-disable-next-line react-hooks/rules-of-hooks
        const [_value, setValue] = useState<string>(value);
        const debouncedOnChange = debounce(onChange, 1000);
        const _onChange = (newValue: string) => {
          setValue(newValue);
          debouncedOnChange(newValue);
        };

        return <Original onChange={_onChange} value={_value} {...props} />;
      },

    RequestBodyEditor:
      (Original: React.FC<RequestBodyEditorProps>) =>
      ({ onChange, ...props }: RequestBodyEditorProps) => {
        const _onChange = debounce(onChange, 1000);

        return <Original onChange={_onChange} {...props} />;
      },
    FilterContainer: () => FilterContainer,
  },
});

// noinspection JSUnusedGlobalSymbols
const ApiBrowserPage = () => (
  <DocumentTitle title="API Browser">
    <StyledSwaggerContainer>
      <SwaggerUI
        url={qualifyUrl('/openapi.yaml')}
        filter
        deepLinking
        defaultModelsExpandDepth={-1}
        requestInterceptor={(req) => ({
          ...req,
          headers: {
            ...req.headers,
            'X-Requested-By': 'API Browser',
          },
        })}
        plugins={[DebounceInputsPlugin]}
      />
    </StyledSwaggerContainer>
  </DocumentTitle>
);

export default ApiBrowserPage;
