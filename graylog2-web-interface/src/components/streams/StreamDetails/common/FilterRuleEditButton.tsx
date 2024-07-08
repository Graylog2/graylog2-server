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
import { useState } from 'react';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';

import FilterRuleForm from './FilterRuleForm';
import type { StreamOutputFilterRule } from './Types';

type Props ={
  filterRule: StreamOutputFilterRule,
};

const FilterRuleEditButton = ({ filterRule }: Props) => {
  const [showForm, setShowForm] = useState(false);

  const onClick = () => {
    setShowForm(true);
  };

  return (
    <>
      <Button bsStyle="link"
              bsSize="xsmall"
              onClick={onClick}
              title="View">
        <Icon name="edit_square" />
      </Button>
      {showForm && <FilterRuleForm title="Edit Filter Rule" filterRule={filterRule} onCancel={() => setShowForm(false)}/>}
    </>
  );
};

export default FilterRuleEditButton;
