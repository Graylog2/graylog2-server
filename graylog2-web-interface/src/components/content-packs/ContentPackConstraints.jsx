import PropTypes from 'prop-types';
import React from 'react';
import { Set } from 'immutable';
import styled from 'styled-components';

import { DataTable, Icon } from 'components/common';
import { Badge } from 'components/graylog';

const StyledBadge = styled(({ isFulfilled, theme, ...rest }) => <Badge {...rest} />)`
  background-color: ${({ isFulfilled, theme }) => (isFulfilled ? theme.color.variant.success : theme.color.variant.danger)};
`;

class ContentPackConstraints extends React.Component {
  static propTypes = {
    constraints: PropTypes.oneOfType([
      PropTypes.object,
      PropTypes.array,
    ]),
    isFulfilled: PropTypes.bool,
  };

  static defaultProps = {
    constraints: Set(),
    isFulfilled: false,
  };

  _rowFormatter = (item) => {
    const { isFulfilled } = this.props;
    const constraint = item.constraint || item;
    constraint.fulfilled = isFulfilled || item.fulfilled;
    const name = constraint.type === 'server-version' ? 'Graylog' : constraint.plugin;

    return (
      <tr key={constraint.id}>
        <td>{name}</td>
        <td>{constraint.type}</td>
        <td>{constraint.version}</td>
        <td>
          <StyledBadge isFulfilled={constraint.fulfilled}>
            <Icon name={constraint.fulfilled ? 'check' : 'times'} />
          </StyledBadge>
        </td>
      </tr>
    );
  };

  render() {
    const { constraints: unfilteredConstraints } = this.props;
    const headers = ['Name', 'Type', 'Version', 'Fulfilled'];
    let constraints = unfilteredConstraints.map((constraint) => {
      const newConstraint = constraint.constraint || constraint;
      newConstraint.fulfilled = constraint.fulfilled;
      return newConstraint;
    });

    if (typeof constraints.toArray === 'function') {
      constraints = constraints.toArray();
    }

    return (
      <div>
        <h2>Constraints</h2>
        <br />
        <br />
        <DataTable id="content-packs-constraints"
                   headers={headers}
                   headerCellFormatter={(header) => <th>{header}</th>}
                   sortBy={(row) => { return row.constraint ? row.constraint.type : row.type; }}
                   dataRowFormatter={this._rowFormatter}
                   rows={constraints}
                   filterKeys={[]} />
      </div>
    );
  }
}

export default ContentPackConstraints;
