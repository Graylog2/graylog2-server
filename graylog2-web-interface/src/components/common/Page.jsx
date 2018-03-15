import PropTypes from 'prop-types';
import React from 'react';
import { Pager } from 'react-bootstrap';

/**
 * Component that encapsulates react-bootstrap's `Pager.Item`.
 *
 * @deprecated Please use `Pager.Item` directly, as this abstraction doesn't provide much value at the moment.
 */
class Page extends React.Component {
  static propTypes = {
    /** href the page should link to. */
    href: PropTypes.string,
    /** Page name or number. */
    page: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
    ]).isRequired,
    /** Callback to be called when the page is selected. It receives the page name or number as argument. */
    onPageChanged: PropTypes.func.isRequired,
    /** Specifies if the link should be disabled or not. */
    isDisabled: PropTypes.bool,
    /** Specifies if the current page is selected or not. */
    isActive: PropTypes.bool,
  };

  render() {
    let className = '';
    if (this.props.isActive) {
      className = 'active';
    }

    return (
      <Pager.Item href={this.props.href}
                className={className}
                disabled={this.props.isDisabled}
                onSelect={() => this.props.onPageChanged(this.props.page)}>
        {this.props.page}
      </Pager.Item>
    );
  }
}

export default Page;
