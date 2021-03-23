import * as React from 'react';
import { Field, useFormikContext } from 'formik';

import Select from 'components/common/Select';
import { Input } from 'components/bootstrap';
import { MetricFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

type Props = {
  index: number,
}

const directionOptions = [
  { label: 'Ascending', value: 'Ascending' },
  { label: 'Descending', value: 'Descending' },
];

const formatSeries = (metric: MetricFormValues) => (metric.name ? metric.name : `${metric.function}(${metric.field ?? ''})`);

const Sort = ({ index }: Props) => {
  const { values } = useFormikContext<WidgetConfigFormValues>();
  const { metrics = [] } = values;

  const metricsOptions = metrics.map(formatSeries).map((metric) => ({ label: metric, value: metric }));

  return (
    <>
      <Field name={`sort.${index}.field`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="field-select"
                 label="Field"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={metricsOptions}
                    clearable={false}
                    name={name}
                    value={value}
                    placeholder="Specify field/metric to be sorted on"
                    aria-label="Select field for sorting"
                    onChange={(newValue) => {
                      onChange({ target: { name, value: newValue } });
                    }} />
          </Input>
        )}
      </Field>

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
