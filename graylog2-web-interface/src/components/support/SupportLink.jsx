import PropTypes from 'prop-types';
import React from 'react';
import { Icon } from 'components/common';

class SupportLink extends React.Component {
  static propTypes = {
    small: PropTypes.bool,
    children: PropTypes.node.isRequired,
  };

  render() {
    return (
      <table className="description-tooltips" style={{ marginBottom: '10px' }}>
        <tbody>
          <tr>
            <td style={{ width: '40px' }}>
              <span className={`fa-stack ${!this.props.small && 'fa-lg'}`}>
                <Icon name="circle" className="fa-stack-2x" />
                <Icon name="lightbulb-o" className="fa-stack-1x" inverse />
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
  }
}

export default SupportLink;
