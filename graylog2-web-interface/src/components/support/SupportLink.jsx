import PropTypes from 'prop-types';
import React from 'react';
import { Icon } from 'components/graylog';

class SupportLink extends React.Component {
  static propTypes = {
    small: PropTypes.bool,
    children: PropTypes.node.isRequired,
  };

  render() {
    const classNames = (this.props.small ? 'fa-stack' : 'fa-stack fa-lg');
    return (
      <table className="description-tooltips" style={{ marginBottom: '10px' }}>
        <tbody>
          <tr>
            <td style={{ width: '40px' }}>
              <span className={classNames}>
                <Icon className="fa fa-circle fa-stack-2x" />
                <Icon className="fa fa-lightbulb-o fa-stack-1x fa-inverse" />
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
