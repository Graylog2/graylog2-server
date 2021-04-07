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
import { FieldArray, useFormikContext } from 'formik';

import { Button, ButtonToolbar } from 'components/graylog';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import ElementConfigurationSection
  from 'views/components/aggregationwizard/elementConfiguration/ElementConfigurationSection';
import Sort from 'views/components/aggregationwizard/elementConfiguration/Sort';

const SortConfiguration = () => {
  const { values } = useFormikContext<WidgetConfigFormValues>();
  const { sort } = values;

  return (
    <FieldArray name="sort"
                render={({ push }) => (
                  <>
                    <div>
                      {sort.map((s, index) => (
                        // eslint-disable-next-line react/no-array-index-key
                        <ElementConfigurationSection key={`sort-${index}`}>
                          <Sort index={index} />
                        </ElementConfigurationSection>
                      ))}
                    </div>
                    <ButtonToolbar>
                      <Button className="pull-right" bsSize="small" type="button" onClick={() => push({})}>
                        Add a Sort
                      </Button>
                    </ButtonToolbar>
                  </>
                )} />
  );
};

export default SortConfiguration;
