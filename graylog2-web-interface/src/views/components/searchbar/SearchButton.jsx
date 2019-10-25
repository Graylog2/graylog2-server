import React from 'react';
import PropTypes from 'prop-types';

import { Button } from 'components/graylog';
import { Spinner, Icon } from 'components/common';

const SearchButton = ({ running, disabled, glyph }) => (
  <Button type="submit" bsStyle={running ? 'warning' : 'success'} disabled={disabled} className="pull-left search-button-execute">
    {running ? <Spinner fixedWidth pulse text="" /> : <Icon name={glyph} /> }
  </Button>
);

SearchButton.defaultProps = {
  running: false,
  disabled: false,
  glyph: 'search',
};

SearchButton.propTypes = {
  running: PropTypes.bool,
  disabled: PropTypes.bool,
  glyph: PropTypes.string,
};

export default SearchButton;
