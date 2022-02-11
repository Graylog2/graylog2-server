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
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';

import FilterAggregationForm from './FilterAggregationForm';

type Props = {
  action: 'create' | 'edit',
  validation: {},
  eventDefinition: {},
  fieldTypes: {},
  onChange: () => void,
  currentUser: {
    permissions: Array<string>,
  }
};

const FilterAggregationFormContainer = (props: Props) => {
  const { data: fieldTypes } = useFieldTypes([], { type: 'relative', range: 0 });
  const isLoading = !fieldTypes;

  if (isLoading) {
    return <Spinner text="Loading Filter & Aggregation Information..." />;
  }

  return <FilterAggregationForm allFieldTypes={fieldTypes} {...props} />;
};

FilterAggregationFormContainer.propTypes = {
  action: PropTypes.oneOf(['create', 'edit']).isRequired,
  validation: PropTypes.object.isRequired,
  eventDefinition: PropTypes.object.isRequired,
  fieldTypes: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  currentUser: PropTypes.object.isRequired, // Prop is passed down to pluggable entities
};

export default FilterAggregationFormContainer;
