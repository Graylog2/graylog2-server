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
import { Trans, useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';
import { Field, useField } from 'formik';
import { useQuery } from '@tanstack/react-query';
import trim from 'lodash/trim';

import { Col, FormControl, FormGroup, Panel, Row } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { testNaturalDate } from 'api/tools';
import useUserDateTime from 'hooks/useUserDateTime';
import { InputDescription, Timestamp } from 'components/common';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import { EMPTY_RANGE } from 'views/components/time-range-picker/TimeRangeDisplay';

const Headline = styled.h3`
  margin-bottom: 5px;
`;

const KeywordInput = styled(FormControl)(
  ({ theme }) => css`
    min-height: 34px;
    font-size: ${theme.fonts.size.large};
  `,
);

const EffectiveTimeRangeTable = styled.table`
  margin-bottom: 5px;

  td:first-child {
    padding-right: 10px;
  }
`;

const debouncedTestNaturalDate = debounceWithPromise(
  (keyword: string, userTZ: string) => testNaturalDate(keyword, userTZ),
  350,
);

const TimePreview = ({ dateTime, isLoading }: { dateTime: string; isLoading: boolean }) => {
  if (!dateTime || isLoading) {
    return <>{EMPTY_RANGE}</>;
  }

  return <Timestamp dateTime={dateTime} format="complete" />;
};

const useKeywordPreview = (keyword: string, userTZ: string) => {
  const { data, isFetching } = useQuery({
    queryKey: ['time-range', 'validation', 'keyword', keyword],
    queryFn: () => debouncedTestNaturalDate(keyword, userTZ),
    retry: 0,
    enabled: !!trim(keyword),
  });

  return { data, isFetching };
};

const KeywordTimeRangePreview = () => {
  const { t } = useTranslation('search');
  const [
    {
      value: { keyword },
    },
  ] = useField('timeRangeTabs.keyword');
  const { userTimezone } = useUserDateTime();
  const { data, isFetching } = useKeywordPreview(keyword, userTimezone);

  return (
    <EffectiveTimeRangeTable>
      <tbody>
        <tr>
          <td>{t('time_range.keyword_from')}</td>
          <td>
            <TimePreview dateTime={data?.from} isLoading={isFetching} />
          </td>
        </tr>
        <tr>
          <td>{t('time_range.keyword_to')}</td>
          <td>
            <TimePreview dateTime={data?.to} isLoading={isFetching} />
          </td>
        </tr>
      </tbody>
    </EffectiveTimeRangeTable>
  );
};

type Props = {
  disabled?: boolean;
};

// Keywords are parsed by the server, which only understands English. They must not be translated.
const KEYWORD_PLACEHOLDER = 'Last week';
const KEYWORD_EXAMPLES = [
  { keyword: 'last month', description: 'time_range.keyword_example_last_month' },
  { keyword: '4 hours ago', description: 'time_range.keyword_example_hours_ago' },
  { keyword: '1st of april to 2 days ago', description: 'time_range.keyword_example_range' },
  { keyword: 'yesterday midnight +0200 to today midnight +0200', description: 'time_range.keyword_example_timezone' },
] as const;

const KeywordDocumentationLink = ({ children = undefined }: React.PropsWithChildren) => {
  const { t } = useTranslation('search');

  return (
    <DocumentationLink
      page={DocsHelper.PAGES.TIME_FRAME_SELECTOR}
      title={t('time_range.keyword_docs_title')}
      text={children}
    />
  );
};

const TabKeywordTimeRange = ({ disabled = false }: Props) => {
  const { t } = useTranslation('search');

  return (
    <Row className="no-bm">
      <Col sm={5}>
        <Headline>{t('time_range.keyword_headline')}</Headline>
        <Field name="timeRangeTabs.keyword.keyword">
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <FormGroup
              controlId="form-inline-keyword"
              style={{ marginRight: 5, width: '100%', marginBottom: 0 }}
              validationState={error ? 'error' : null}>
              <KeywordInput
                type="text"
                className="input-sm mousetrap"
                name={name}
                disabled={disabled}
                placeholder={KEYWORD_PLACEHOLDER}
                title={t('time_range.keyword_input')}
                aria-label={t('time_range.keyword_input')}
                onChange={onChange}
                required
                value={value}
              />
              <InputDescription error={error} help={t('time_range.keyword_help')} />
            </FormGroup>
          )}
        </Field>

        <b>{t('time_range.keyword_preview')}</b>
        <KeywordTimeRangePreview />
      </Col>

      <Col sm={7}>
        <Panel>
          <Panel.Body>
            {KEYWORD_EXAMPLES.map(({ keyword, description }) => (
              <p key={keyword}>
                <Trans t={t} i18nKey={description} values={{ keyword }} components={{ code: <code /> }} />
              </p>
            ))}

            <p>
              <Trans t={t} i18nKey="time_range.keyword_docs" components={{ docsLink: <KeywordDocumentationLink /> }} />
            </p>
          </Panel.Body>
        </Panel>
      </Col>
    </Row>
  );
};

export default TabKeywordTimeRange;
