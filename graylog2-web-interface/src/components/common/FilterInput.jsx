import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';

/**
 * The FilterInput component will render a filter input and takes a
 * list or object to be filtered and a filter function which will be
 * executed while typing.
 */
class FilterInput extends React.Component {
  static propTypes = {
    /**
     * This callback will be called when the user types in a filter string.
     * It takes the new filter string as an argument.
     */
    onChange: PropTypes.func.isRequired,
    /** The optional label for the filter. Default is 'Filter' */
    filterLabel: PropTypes.string,
    /** The wrapperClassName of the Input for styling */
    wrapperClassName: PropTypes.string,
    /** The labelClassName of the Input for styling */
    labelClassName: PropTypes.string,
  };

  static defaultProps = {
    filterLabel: 'Filter',
    wrapperClassName: 'col-sm-4',
    labelClassName: 'col-sm-1',
  };

  constructor(props) {
    super(props);

    this.state = {
      filter: '',
    };
  }

  _onChange = (e) => {
    const filter = e.target.value;
    this.setState({ filter: filter });
    this.props.onChange(filter);
  };

  render() {
    return (
      <form className="form-horizontal" onSubmit={(e) => { e.preventDefault(); }}>
        <Input
          id="filter-descriptors"
          type="text"
          wrapperClassName={this.props.wrapperClassName}
          labelClassName={this.props.labelClassName}
          label={this.props.filterLabel}
          value={this.state.filter}
          onChange={this._onChange}
        />
      </form>
    );
  }
}

export default FilterInput;
