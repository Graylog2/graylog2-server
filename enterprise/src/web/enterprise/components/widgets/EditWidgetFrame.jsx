import React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'react-bootstrap';

// eslint-disable-next-line import/no-webpack-loader-syntax
import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';

const EditWidgetDialog = ({ children, ...rest }) => <Modal.Dialog {...rest} dialogClassName={styles.editWidgetDialog}>{children}</Modal.Dialog>;

EditWidgetDialog.propTypes = {
  children: PropTypes.node.isRequired,
};

const EditWidgetFrame = ({ children }) => (
  <Modal show animation={false} dialogComponentClass={EditWidgetDialog} enforceFocus={false}>
    <div style={{ height: 'calc(100% - 20px)' }}>
      {children}
    </div>
  </Modal>
);

EditWidgetFrame.propTypes = {
  children: PropTypes.node.isRequired,
};

export default EditWidgetFrame;
