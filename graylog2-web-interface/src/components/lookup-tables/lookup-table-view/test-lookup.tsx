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
import { useState, useMemo } from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import * as JSON from 'util/json';
import useProductName from 'brand-customization/useProductName';
import { useErrorsContext } from 'components/lookup-tables/contexts/ErrorsContext';
import { Col, Row, DataWell } from 'components/lookup-tables/layout-componets';
import { Button, Input, Alert } from 'components/bootstrap';
import { useFetchLookupPreview, useTestLookupTableKey } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTable } from 'logic/lookup-tables/types';

import { Description } from './lookup-table-show';

const StyledDataWell = styled(DataWell)`
  line-height: normal;
  white-space: pre;
  font-family: monospace;
  font-size: medium;
  color: ${({ theme }) => (theme.mode === 'light' ? 'darkslateblue' : 'lightsteelblue')};
  overflow: auto;
  max-height: 200px;
`;

const NoMarginInput = styled.div`
  & .form-group {
    margin-bottom: 0;
  }
`;

const StyledAlert = styled(Alert)`
  width: 100%;
`;

const INIT_INPUT = { value: '', valid: false };

type Props = {
  table: LookupTable;
};

function TestLookup({ table }: Props) {
  const { t } = useTranslation('lookup_tables');
  const { errors } = useErrorsContext();
  const lutError = errors?.lutErrors[table.name];
  const [lookupKey, setLookupKey] = useState<{ value: string; valid: boolean }>(INIT_INPUT);
  const [lookupResult, setLookupResult] = useState<any>(null);
  const [previewSize, setPreviewSize] = useState<number>(5);
  const productName = useProductName();
  const { testLookupTableKey } = useTestLookupTableKey();
  const {
    lookupPreview: { results, total, supported },
  } = useFetchLookupPreview(table.id, !lutError, previewSize);

  const dataPreview = useMemo(() => {
    if (total === 0) return t('lut_view.no_results');

    return JSON.stringify(results, null, 2);
  }, [results, total, t]);

  const onChange = (event: React.BaseSyntheticEvent) => {
    const newValue = { ...lookupKey };
    newValue.valid = event.target.value && event.target.value.replace(/\s/g, '').length > 0;
    newValue.value = event.target.value;

    setLookupKey(newValue);
    setLookupResult(null);
  };

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement> & React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Backspace') setLookupResult(null);
  };

  const onReset = () => {
    setLookupKey(INIT_INPUT);
    setLookupResult(null);
  };

  const handleLookupKey = (event: React.SyntheticEvent) => {
    event.preventDefault();

    if (lookupKey.valid) {
      testLookupTableKey({ tableName: table.name, key: lookupKey.value }).then((resp: any) => {
        setLookupResult(JSON.stringify(resp, null, 2));
        setLookupKey({ value: '', valid: false });
      });
    }
  };

  const onPreviewSizeChange = ({ target: { value } }: React.BaseSyntheticEvent) => {
    const auxValue = value ? +value : value;
    if (typeof auxValue !== 'number') {
      setPreviewSize(auxValue);
    } else if (auxValue < 1) {
      setPreviewSize(1);
    } else {
      setPreviewSize(auxValue);
    }
  };

  const showResults = useMemo(() => !lutError && (lookupResult || supported), [lutError, lookupResult, supported]);

  return (
    <Col $gap="sm">
      <Col $gap="xs">
        <h2>{t('lut_view.test_title')}</h2>
        <Description>{t('lut_view.test_description', { productName })}</Description>
      </Col>
      <Col $gap="xs">
        <form onSubmit={handleLookupKey} style={{ width: '100%' }}>
          <fieldset>
            <Input
              type="text"
              id="key"
              name="lookupkey"
              placeholder={t('lut_view.test_placeholder')}
              label={t('lut_view.key_label')}
              required
              onKeyDown={onKeyDown}
              onChange={onChange}
              help={t('adapter_summary.key_help')}
              value={lookupKey.value}
            />
            <Row $justify="flex-end">
              <Button name="reset" disabled={!lookupResult} onClick={onReset}>
                {t('lut_view.reset')}
              </Button>
              <Button
                type="submit"
                name="lookupbutton"
                bsStyle="primary"
                data-testid="lookupbutton"
                disabled={!lookupKey.valid}>
                {t('adapter_summary.look_up')}
              </Button>
            </Row>
          </fieldset>
        </form>
      </Col>
      {lutError && <StyledAlert bsStyle="danger">{lutError}</StyledAlert>}
      {showResults && (
        <Col $gap="xs" style={{ marginTop: 20 }}>
          <h4 style={{ width: '100%' }}>
            <Row $align="center" $justify="space-between">
              <span>{t('adapter_summary.lookup_result')}</span>
              {supported && total > 0 && !lookupResult && (
                <Row $width="auto" $align="center">
                  <NoMarginInput>
                    <Input
                      type="number"
                      bsSize="sm"
                      onChange={onPreviewSizeChange}
                      value={previewSize > total ? total : previewSize}
                      style={{ marginLeft: 'auto' }}
                      min={1}
                      max={total}
                    />
                  </NoMarginInput>
                  <Description>{t('lut_view.of')}</Description>
                  <Description>{total}</Description>
                </Row>
              )}
            </Row>
          </h4>
          <StyledDataWell>{lookupResult ?? dataPreview}</StyledDataWell>
        </Col>
      )}
    </Col>
  );
}

export default TestLookup;
