// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'react-bootstrap';

// eslint-disable-next-line import/no-webpack-loader-syntax
import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';
// eslint-disable-next-line import/no-webpack-loader-syntax
import globalStyles from '!style/useable!css!./EditWidgetFrame.global.css';

const EditWidgetDialog = ({ children, ...rest }) => <Modal.Dialog {...rest} dialogClassName={styles.editWidgetDialog}>{children}</Modal.Dialog>;

EditWidgetDialog.propTypes = {
  children: PropTypes.node.isRequired,
};

const catchEvent = e => e.stopPropagation();

type Props = {
  children: React.Node,
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
    const { children } = this.props;
    return (
      <Modal show animation={false} dialogComponentClass={EditWidgetDialog} enforceFocus={false}>
        <div role="presentation" style={{ height: 'calc(100% - 20px)' }} onMouseDown={catchEvent}>
          {children}
        </div>
      </Modal>
    );
  }
}
