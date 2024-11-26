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
import { useContext } from 'react';
import Immutable from 'immutable';

import FieldSelectBase from 'views/components/aggregationwizard/FieldSelectBase';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

type Props = Omit<React.ComponentProps<typeof FieldSelectBase>, 'options'>

const FieldSelect = (props: Props) => {
  const activeQuery = useActiveQueryId();
  const fieldTypes = useContext(FieldTypesContext);
  const fieldOptions = fieldTypes.queryFields.get(activeQuery, Immutable.List()).toArray();

  return (
    <FieldSelectBase options={fieldOptions}
                     {...props} />
  );
};

export default FieldSelect;
