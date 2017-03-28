import React, { PropTypes } from 'react';
import { Pager } from 'react-bootstrap';

const Page = React.createClass({
  propTypes: {
    href: PropTypes.string,
    page: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
    ]).isRequired,
    onPageChanged: PropTypes.func.isRequired,
    isDisabled: PropTypes.bool,
    isActive: PropTypes.bool,
  },
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
  },
});

export default Page;
