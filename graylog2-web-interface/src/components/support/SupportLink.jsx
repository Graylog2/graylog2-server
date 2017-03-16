import React from 'react';

const SupportLink = React.createClass({
  propTypes: {
    small: React.PropTypes.bool,
    children: React.PropTypes.node.isRequired,
  },
  render() {
    const classNames = (this.props.small ? 'fa-stack' : 'fa-stack fa-lg');
    return (
      <table className="description-tooltips" style={{ marginBottom: '10px' }}>
        <tbody>
          <tr>
            <td style={{ width: '40px' }}>
              <span className={classNames}>
                <i className="fa fa-circle fa-stack-2x" />
                <i className="fa fa-lightbulb-o fa-stack-1x fa-inverse" />
              </span>
            </td>
            <td>
              <strong>
                {this.props.children}
              </strong>
            </td>
          </tr>
        </tbody>
      </table>
    );
  },
});

export default SupportLink;
