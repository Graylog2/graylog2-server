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
import React, { useMemo, useCallback } from 'react';
import { Set } from 'immutable';

import { DataTable, Icon } from 'components/common';
import { Badge } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

type ContentPackConstraintsProps = {
  constraints?: any | any[];
  isFulfilled?: boolean;
};

const ContentPackConstraints = ({ constraints = Set(), isFulfilled = false }: ContentPackConstraintsProps) => {
  const productName = useProductName();
  const headers = useMemo(() => ['Name', 'Type', 'Version', 'Fulfilled'], []);

  const renderHeader = useCallback((header: React.ReactNode) => <th>{header}</th>, []);

  const formattedConstraints = useMemo(() => {
    let updatedConstraints = constraints.map((constraint) => {
      const newConstraint = constraint.constraint || constraint;
      newConstraint.fulfilled = constraint.fulfilled;

      return newConstraint;
    });

    if (typeof updatedConstraints.toArray === 'function') {
      updatedConstraints = updatedConstraints.toArray();
    }

    return updatedConstraints;
  }, [constraints]);

  const rowFormatter = (item) => {
    const constraint = item.constraint || item;
    constraint.fulfilled = isFulfilled || constraint.fulfilled;
    const name = constraint.type === 'server-version' ? productName : constraint.plugin;

    return (
      <tr key={constraint.id}>
        <td>{name}</td>
        <td>{constraint.type}</td>
        <td>{constraint.version}</td>
        <td>
          <Badge bsStyle={constraint.fulfilled ? 'success' : 'danger'}>
            <Icon name={constraint.fulfilled ? 'check_circle' : 'cancel'} />
          </Badge>
        </td>
      </tr>
    );
  };

  return (
    <div>
      <h2>Constraints</h2>
      <br />
      <br />
      <DataTable
        id="content-packs-constraints"
        headers={headers}
        headerCellFormatter={renderHeader}
        sortBy={(row) => (row.constraint ? row.constraint.type : row.type)}
        dataRowFormatter={rowFormatter}
        rows={formattedConstraints}
        filterKeys={[]}
      />
    </div>
  );
};

export default ContentPackConstraints;
