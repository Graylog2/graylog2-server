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

import { Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';

const ColumnPivotConfiguration = ({ onClose, onRollupChange, rollup }) => (
  <div>
    <Input type="checkbox"
           id="rollup"
           name="rollup"
           label="Rollup"
           autoFocus
           onChange={(e) => onRollupChange(e.target.checked)}
           help="When rollup is enabled, an additional trace totalling individual subtraces will be included."
           checked={rollup} />
    <div className="pull-right" style={{ marginBottom: '10px' }}>
      <Button bsStyle="success" onClick={onClose}>Done</Button>
    </div>
  </div>
);

ColumnPivotConfiguration.propTypes = {
  onClose: PropTypes.func,
  onRollupChange: PropTypes.func,
  rollup: PropTypes.bool,
};

ColumnPivotConfiguration.defaultProps = {
  onClose: () => {},
  onRollupChange: () => {},
  rollup: true,
};

export default ColumnPivotConfiguration;
