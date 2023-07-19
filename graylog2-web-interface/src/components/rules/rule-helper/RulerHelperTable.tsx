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
import React from 'react';
import PropTypes from 'prop-types';

import { Table } from 'components/bootstrap';
import { Icon } from 'components/common';

import RuleHelperStyle from './RuleHelper.css';
import { functionSignature, niceType } from './helpers';

import { blockDictPropType } from '../rule-builder/types';
import type { BlockDict } from '../rule-builder/types';

type Props = {
  entries: Array<BlockDict>,
  expanded?: {[key: string] : boolean},
  onFunctionClick?: (functionName: string) => void
}

const RuleHelperTable = ({ entries, expanded, onFunctionClick } : Props) => {
  const parameters = (descriptor) => descriptor.params.map((p) => (
    <tr key={p.name}>
      <td className={RuleHelperStyle.adjustedTableCellWidth}>{p.name}</td>
      <td className={RuleHelperStyle.adjustedTableCellWidth}>{niceType(p.type)}</td>
      <td className={`${RuleHelperStyle.adjustedTableCellWidth} text-centered`}>{p.optional ? null : <Icon name="check" />}</td>
      <td>{p.description}</td>
    </tr>
  ));

  const renderFunctions = (descriptors) => {
    if (!descriptors) {
      return [];
    }

    return descriptors.map((d) => {
      let details = null;

      if (expanded[d.name]) {
        details = (
          <tr>
            <td colSpan={2}>
              <Table condensed striped hover>
                <thead>
                  <tr>
                    <th>Parameter</th>
                    <th>Type</th>
                    <th>Required</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {parameters(d)}
                </tbody>
              </Table>
            </td>
          </tr>
        );
      }

      return (
        <tbody key={d.name}>
          {onFunctionClick ? (
            <tr onClick={() => onFunctionClick(d.name)} className={RuleHelperStyle.clickableRow}>
              <td className={RuleHelperStyle.functionTableCell}><code>{functionSignature(d)}</code></td>
              <td>{d.description}</td>
            </tr>
          ) : (
            <tr>
              <td className={RuleHelperStyle.functionTableCell}><code>{functionSignature(d)}</code></td>
              <td>{d.description}</td>
            </tr>
          )}
          {details}
        </tbody>
      );
    });
  };

  return (
    <Table condensed>
      <thead>
        <tr>
          <th>Function</th>
          <th>Description</th>
        </tr>
      </thead>
      {renderFunctions(entries)}
    </Table>
  );
};

RuleHelperTable.propTypes = {
  entries: PropTypes.arrayOf(blockDictPropType).isRequired,
  expanded: PropTypes.objectOf(PropTypes.bool),
  onFunctionClick: PropTypes.func,
};

RuleHelperTable.defaultProps = {
  expanded: {},
  onFunctionClick: undefined,
};

export default RuleHelperTable;
