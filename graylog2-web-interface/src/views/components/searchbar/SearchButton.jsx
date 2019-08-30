import React from 'react';
import PropTypes from 'prop-types';

import { Button } from 'components/graylog';

const SearchButton = ({ running, disabled }) => (
  <Button type="submit" bsStyle={running ? 'warning' : 'success'} disabled={disabled} className="pull-left search-button-execute">
    <i className={running ? 'fa fa-spinner fa-pulse fa-fw' : 'fa fa-search'} />
  </Button>
);

SearchButton.defaultProps = {
  running: false,
  disabled: false,
};

SearchButton.propTypes = {
  running: PropTypes.bool,
  disabled: PropTypes.bool,
};

export default SearchButton;
