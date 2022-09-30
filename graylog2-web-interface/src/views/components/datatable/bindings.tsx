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

import type { VisualizationType } from 'views/types';
import DataTable from 'views/components/datatable/DataTable';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';

type DataTableVisualizationConfigFormValues = {
  pinned_columns: Array<string>,
};
const dataTable: VisualizationType<typeof DataTable.type> = {
  type: DataTable.type,
  displayName: 'Data Table',
  component: DataTable,
  config: {
    createConfig: () => ({ pinned_columns: [] }),
    fromConfig: (config: DataTableVisualizationConfig | undefined) => ({ pinned_columns: config?.pinned_columns.toJS() ?? [] }),
    toConfig: (formValues: DataTableVisualizationConfigFormValues) => DataTableVisualizationConfig.create(formValues.pinned_columns),
    fields: [{
      name: 'pinned_columns',
      title: 'Pinned Columns',
      type: 'multi-select',
      options: ({ formValues }: { formValues: WidgetConfigFormValues }) => {
        const options = formValues?.groupBy?.groupings.reduce((res, cur) => {
          if (cur.direction === 'row') {
            res.push(cur.field.field);
          }

          return res;
        }, []);
        formValues.metrics.forEach((metric) => options.push(`${metric.function}(${metric.field})`));

        return options || [];
      },
      required: false,
    }],
  },

};

export default dataTable;
