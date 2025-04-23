import React, { useMemo } from 'react';
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
        headerCellFormatter={(header) => <th>{header}</th>}
        sortBy={(row) => (row.constraint ? row.constraint.type : row.type)}
        dataRowFormatter={rowFormatter}
        rows={formattedConstraints}
        filterKeys={[]}
      />
    </div>
  );
};

export default ContentPackConstraints;
