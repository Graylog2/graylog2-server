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
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';

import DataTable from './DataTable';

type DataTableVisualizationConfigFormValues = {
  pinnedColumns: Array<string>,
};
const dataTable: VisualizationType<typeof DataTable.type> = {
  type: DataTable.type,
  displayName: 'Data Table',
  component: DataTable,
  config: {
    createConfig: () => ({ pinnedColumns: [] }),
    fromConfig: (config: DataTableVisualizationConfig | undefined) => ({ pinnedColumns: config?.pinnedColumns.toJS() ?? [] }),
    toConfig: (formValues: DataTableVisualizationConfigFormValues) => DataTableVisualizationConfig.create(formValues.pinnedColumns),
    fields: [{
      name: 'pinnedColumns',
      title: 'Pinned Columns',
      type: 'multi-select',
      options: ({ formValues }: { formValues: WidgetConfigFormValues }) => formValues?.groupBy?.groupings
        .filter((grouping) => (grouping?.direction === 'row' && grouping?.fields))
        .flatMap((grouping) => grouping.fields) ?? [],
      required: false,
    }],
  },

};

export default dataTable;
