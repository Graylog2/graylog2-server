import * as React from 'react';
import { Field } from 'formik';

import FormikInput from 'components/common/FormikInput';
import Select from 'components/common/Select';
import { Input } from 'components/bootstrap';

type Props = {
  index: number,
}

const directionOptions = [
  { label: 'Ascending', value: 'Ascending' },
  { label: 'Descending', value: 'Descending' },
];

const Sort = ({ index }: Props) => {
  return (
    <>
      <FormikInput id="field"
                   label="Field"
                   placeholder="Specify field/metric to be sorted on"
                   name={`sort.${index}.field`}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9" />

      <Field name={`sort.${index}.direction`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="direction-select"
                 label="Direction"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={directionOptions}
                    clearable={false}
                    name={name}
                    value={value}
                    aria-label="Select direction for sorting"
                    onChange={(newValue) => {
                      onChange({ target: { name, value: newValue } });
                    }} />
          </Input>
        )}
      </Field>
    </>
  );
};

export default Sort;
