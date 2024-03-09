import * as React from 'react';
import styled from 'styled-components';
import { useMemo } from 'react';
import { Field } from 'formik';

import { Select } from 'components/common';
import { Input } from 'components/bootstrap';
import { defaultCompare } from 'logic/DefaultCompare';

const Container = styled.div`
  display: flex;
  flex-direction: column;
`;

type Props = {
  columnTitle?: (column: string) => string,
  columns: Array<string>,
  directionTitle?: (direction: string) => string,
  directions: Array<string>,
  name: string,
}

const SortConfiguration = ({ name: attributeName, directions, columns, columnTitle, directionTitle }: Props) => {
  const columnOptions = useMemo(() => (
    columns
      .map((col) => ({ value: col, label: columnTitle(col) }))
      .sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2))
  ), [columnTitle, columns]);

  const directionOptions = useMemo(() => (
    directions
      .map((col) => ({ value: col, label: directionTitle(col) }))
      .sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2))
  ), [directionTitle, directions]);

  return (
    <Container>
      <Field name={`${attributeName}.field`}>
        {({ field: { value, onChange, name } }) => (
          <Input id="sort-configuration-column"
                 label="Column"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select id="sort-configuration-column-select"
                    placeholder="Select a column"
                    options={columnOptions}
                    matchProp="label"
                    menuPortalTarget={document.body}
                    clearable={false}
                    size="small"
                    onChange={(newColumn) => onChange({ target: { value: newColumn, name } })}
                    value={value} />
          </Input>
        )}
      </Field>

      <Field name={`${attributeName}.direction`}>
        {({ field: { value, onChange, name } }) => (
          <Input id="sort-configuration-direction"
                 label="Direction"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select id="sort-configuration-direction-select"
                    placeholder="Select a direction"
                    options={directionOptions}
                    menuPortalTarget={document.body}
                    matchProp="label"
                    clearable={false}
                    size="small"
                    onChange={(newDirection) => onChange({ target: { value: newDirection, name } })}
                    value={value} />
          </Input>
        )}
      </Field>
    </Container>
  );
};

SortConfiguration.defaultProps = {
  columnTitle: (column: string) => column,
  directionTitle: (direction: string) => direction,
};

export default SortConfiguration;
