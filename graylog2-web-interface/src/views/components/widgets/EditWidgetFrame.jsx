// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'components/graylog';

// eslint-disable-next-line import/no-webpack-loader-syntax
import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';
// eslint-disable-next-line import/no-webpack-loader-syntax
import globalStyles from '!style/useable!css!./EditWidgetFrame.global.css';
import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';


const EditWidgetDialog = ({ className, children, widget, ...rest }) => (
  <div {...rest} className={`${className} modal`} style={{ display: 'block' }}>
    <IfDashboard>
      <div className={`${styles.editWidgetControls} modal-dialog`}>
        <div className={`${styles.editWidgetControlsContent} modal-content`} role="document">
          <WidgetQueryControls widget={widget} />
        </div>
      </div>
    </IfDashboard>
    <div className={`${styles.editWidgetDialog} modal-dialog`}>
      <div className="modal-content" role="document">
        {children}
      </div>
    </div>
  </div>
);

EditWidgetDialog.propTypes = {
  className: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
};

type Props = {
  children: Array<React.Node>,
};

export default class EditWidgetFrame extends React.Component<Props> {
  static propTypes = {
    children: PropTypes.node.isRequired,
  };

  componentWillMount() {
    globalStyles.use();
  }

  componentWillUnmount() {
    globalStyles.unuse();
  }

  render() {
    const { children, widget } = this.props;
    return (
      <Modal show
             animation={false}
             dialogComponentClass={({ children, ...props }) => <EditWidgetDialog {...props} widget={widget}>{children}</EditWidgetDialog>} 
             enforceFocus={false}>
        <Modal.Body style={{ height: 'calc(100% - 50px)' }}>
          <div role="presentation" style={{ height: 'calc(100% - 20px)' }}>
            {children[0]}
          </div>
        </Modal.Body>
        <Modal.Footer>
          {children[1]}
        </Modal.Footer>
      </Modal>
    );
  }
}
