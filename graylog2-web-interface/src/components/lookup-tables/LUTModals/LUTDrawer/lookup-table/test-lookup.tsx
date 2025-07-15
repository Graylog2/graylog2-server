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
import styled from 'styled-components';

import useProductName from 'brand-customization/useProductName';
import { useErrorsContext } from 'components/lookup-tables/contexts/ErrorsContext';
import { Col, Row, DataWell } from 'components/lookup-tables/layout-componets';
import { Button, Input, Alert } from 'components/bootstrap';
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';
import { useFetchLookupPreview } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTable } from 'logic/lookup-tables/types';

import { Description } from '.';

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
  const { errors } = useErrorsContext();
  const lutError = errors?.lutErrors[table.name];
  const [lookupKey, setLookupKey] = React.useState<{ value: string; valid: boolean }>(INIT_INPUT);
  const [lookupResult, setLookupResult] = React.useState<any>(null);
  const [previewSize, setPreviewSize] = React.useState<number>(5);
  const productName = useProductName();
  const {
    lookupPreview: { results, total, supported },
  } = useFetchLookupPreview(table.id, !lutError, previewSize);

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
      LookupTablesActions.lookup(table.name, lookupKey.value).then((resp: any) => {
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

  return (
    <Col $gap="sm">
      <Col $gap="xs">
        <h2>Test lookup</h2>
        <Description>
          You can manually query the lookup table using this form. The data will be cached as configured by{' '}
          {productName}.
        </Description>
      </Col>
      {lutError && <StyledAlert bsStyle="danger">{lutError}</StyledAlert>}
      {!supported && !lutError && (
        <StyledAlert bsStyle="warning">This lookup table doesn&apos;t support keys preview</StyledAlert>
      )}
      {supported && !lutError && total < 1 && <StyledAlert>No result to show</StyledAlert>}
      {supported && !lutError && total > 0 && (
        <Col $gap="xs">
          <form onSubmit={handleLookupKey} style={{ width: '100%' }}>
            <fieldset>
              <Input
                type="text"
                id="key"
                name="lookupkey"
                placeholder="Insert key that should be looked up"
                label="Key"
                required
                onKeyDown={onKeyDown}
                onChange={onChange}
                help="Key to look up a value for."
                value={lookupKey.value}
              />
              <Row $justify="flex-end">
                <Button name="reset" disabled={!lookupResult} onClick={onReset}>
                  Reset
                </Button>
                <Button type="submit" name="lookupbutton" bsStyle="primary" disabled={!lookupKey.valid}>
                  Look up
                </Button>
              </Row>
            </fieldset>
          </form>
          <Col $gap="xs" style={{ marginTop: 20 }}>
            <h4 style={{ width: '100%' }}>
              <Row $align="center" $justify="space-between">
                <span>Lookup result</span>
                <Row $width="auto" $align="center">
                  <NoMarginInput>
                    <Input
                      type="number"
                      bsSize="xs"
                      onChange={onPreviewSizeChange}
                      value={previewSize > total ? total : previewSize}
                      style={{ marginLeft: 'auto' }}
                      min={1}
                      max={total}
                    />
                  </NoMarginInput>
                  <Description>of</Description>
                  <Description>{total}</Description>
                </Row>
              </Row>
            </h4>
            <StyledDataWell>{lookupResult ?? JSON.stringify(results, null, 2)}</StyledDataWell>
          </Col>
        </Col>
      )}
    </Col>
  );
}

export default TestLookup;
