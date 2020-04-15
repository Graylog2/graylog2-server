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
