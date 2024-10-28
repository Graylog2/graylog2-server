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
import styled, { css } from 'styled-components';
import { Field, useField } from 'formik';
import { useQuery } from '@tanstack/react-query';
import trim from 'lodash/trim';

import { Col, FormControl, FormGroup, Panel, Row } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import ToolsStore from 'stores/tools/ToolsStore';
import useUserDateTime from 'hooks/useUserDateTime';
import { InputDescription, Timestamp } from 'components/common';
import debounceWithPromise from 'views/logic/debounceWithPromise';

import { EMPTY_RANGE } from '../TimeRangeDisplay';

const Headline = styled.h3`
  margin-bottom: 5px;
`;

const KeywordInput = styled(FormControl)(({ theme }) => css`
  min-height: 34px;
  font-size: ${theme.fonts.size.large};
`);

const EffectiveTimeRangeTable = styled.table`
  margin-bottom: 5px;

  td:first-child {
    padding-right: 10px;
  }
`;

const debouncedTestNaturalDate = debounceWithPromise((
  keyword: string,
  userTZ: string,
) => ToolsStore.testNaturalDate(keyword, userTZ), 350);

const TimePreview = ({ dateTime, isLoading }: { dateTime: string, isLoading: boolean }) => {
  if (!dateTime || isLoading) {
    return <>{EMPTY_RANGE}</>;
  }

  return <Timestamp dateTime={dateTime} format="complete" />;
};

const useKeywordPreview = (keyword: string, userTZ: string) => {
  const { data, isFetching } = useQuery(['time-range', 'validation', 'keyword', keyword], () => debouncedTestNaturalDate(keyword, userTZ), {
    retry: 0,
    enabled: !!trim(keyword),
  });

  return { data, isFetching };
};

const KeywordTimeRangePreview = () => {
  const [{ value: { keyword } }] = useField('timeRangeTabs.keyword');
  const { userTimezone } = useUserDateTime();
  const { data, isFetching } = useKeywordPreview(keyword, userTimezone);

  return (
    <EffectiveTimeRangeTable>
      <tbody>
        <tr>
          <td>From</td>
          <td>
            <TimePreview dateTime={data?.from} isLoading={isFetching} />
          </td>
        </tr>
        <tr>
          <td>To</td>
          <td>
            <TimePreview dateTime={data?.to} isLoading={isFetching} />
          </td>
        </tr>
      </tbody>
    </EffectiveTimeRangeTable>
  );
};

type Props = {
  disabled?: boolean
};

const TabKeywordTimeRange = ({ disabled = false }: Props) => (
  <Row className="no-bm">
    <Col sm={5}>
      <Headline>Time range:</Headline>
      <Field name="timeRangeTabs.keyword.keyword">
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <FormGroup controlId="form-inline-keyword"
                     style={{ marginRight: 5, width: '100%', marginBottom: 0 }}
                     validationState={error ? 'error' : null}>
            <KeywordInput type="text"
                          className="input-sm mousetrap"
                          name={name}
                          disabled={disabled}
                          placeholder="Last week"
                          title="Keyword input"
                          aria-label="Keyword input"
                          onChange={onChange}
                          required
                          value={value} />
            <InputDescription error={error} help="Specify the time frame for the search in natural language." />
          </FormGroup>
        )}
      </Field>

      <b>Preview</b>
      <KeywordTimeRangePreview />
    </Col>

    <Col sm={7}>
      <Panel>
        <Panel.Body>
          <p><code>last month</code> searches between one month ago and now</p>

          <p><code>4 hours ago</code> searches between four hours ago and now</p>

          <p><code>1st of april to 2 days ago</code> searches between 1st of April and 2 days ago</p>

          <p><code>yesterday midnight +0200 to today midnight +0200</code> searches between yesterday midnight and today midnight in timezone +0200 - will be 22:00 in UTC</p>

          <p>Please consult the <DocumentationLink page={DocsHelper.PAGES.TIME_FRAME_SELECTOR}
                                                   title="Keyword Time Range Documentation"
                                                   text="documentation" /> for more details.
          </p>
        </Panel.Body>
      </Panel>
    </Col>
  </Row>
);

export default TabKeywordTimeRange;
