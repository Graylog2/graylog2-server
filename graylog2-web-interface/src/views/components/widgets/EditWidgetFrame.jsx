// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'components/graylog';

import WidgetContext from 'views/components/contexts/WidgetContext';
import QueryEditModeContext from 'views/components/contexts/QueryEditModeContext';
import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';

// eslint-disable-next-line import/no-webpack-loader-syntax
import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';
// eslint-disable-next-line import/no-webpack-loader-syntax
import globalStyles from '!style/useable!css!./EditWidgetFrame.global.css';
import HeaderElements from '../HeaderElements';
import QueryBarElements from '../QueryBarElements';
import WidgetOverrideElements from '../WidgetOverrideElements';


type DialogProps = {
  bsClass: string,
  className: string,
  children: React.Node,
};

const EditWidgetDialog = ({ className, children, bsClass, ...rest }: DialogProps) => (
  <Modal.Dialog {...rest} dialogClassName={styles.editWidgetDialog}>
    <div className={styles.gridContainer}>
      {children}
    </div>
  </Modal.Dialog>
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
    const { children } = this.props;
    return (
      <Modal show
             animation={false}
             dialogComponentClass={EditWidgetDialog}
             enforceFocus={false}>
        <IfDashboard>
          <Modal.Header className={styles.QueryControls}>
            <QueryEditModeContext.Provider value="widget">
              <HeaderElements />
              <WidgetContext.Consumer>
                {widget => (
                  <WidgetQueryControls widget={widget} />
                )}
              </WidgetContext.Consumer>
            </QueryEditModeContext.Provider>
          </Modal.Header>
        </IfDashboard>
        <Modal.Body className={styles.Visualization}>
          <div role="presentation" style={{ height: '100%' }}>
            <WidgetOverrideElements>
              {children[0]}
            </WidgetOverrideElements>
          </div>
        </Modal.Body>
        <Modal.Footer className={styles.Footer}>
          {children[1]}
        </Modal.Footer>
      </Modal>
    );
  }
}
