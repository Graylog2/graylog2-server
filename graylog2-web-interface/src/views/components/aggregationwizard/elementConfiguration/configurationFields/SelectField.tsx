import * as React from 'react';
import { Field } from 'formik';
import { SelectField as SelectFieldType } from 'views/types';

import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import { HoverForHelp } from 'components/common';

type Props = {
  field: SelectFieldType,
  name: string,
}

const makeOptions = (options: Array<string | [string, any]>) => {
  return options.map((option) => {
    if (typeof option === 'string') {
      return { label: option, value: option };
    }

    const [label, value] = option;

    return { label, value };
  });
};

const SelectField = ({ name: namePrefix, field }: Props) => {
  const { helpComponent: HelpComponent } = field;
  const title = HelpComponent
    ? <>{field.title}<HoverForHelp title={`Help for ${field.title}`}><HelpComponent /></HoverForHelp></>
    : field.title;

  return (
    <Field name={`${namePrefix}.${field.name}`}>
      {({ field: { name, value, onChange }, meta: { error } }) => (
        <Input id={`${name}-select`}
               label={title}
               error={error}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select options={makeOptions(field.options)}
                  clearable={!field.required}
                  name={name}
                  value={value}
                  onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
        </Input>
      )}
    </Field>
  );
};

export default SelectField;
