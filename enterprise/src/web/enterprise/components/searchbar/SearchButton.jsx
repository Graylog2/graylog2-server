import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'react-bootstrap';

const SearchButton = ({ running }) => (
  <Button type="submit" bsStyle={running ? 'warning' : 'success'} className="pull-left">
    <i className={running ? 'fa fa-spinner fa-pulse fa-fw' : 'fa fa-search'} />
  </Button>
);

SearchButton.defaultProps = {
  running: false,
};

SearchButton.propTypes = {
  running: PropTypes.bool,
};

export default SearchButton;
