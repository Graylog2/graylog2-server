import React from 'react';

import styles from './TemplateFieldValueProviderPreview.css';

class TemplateFieldValueProviderPreview extends React.Component {
  static propTypes = {};

  render() {
    return (
      <div className={styles.templatePreview}>
        <h3>Available Fields in Template</h3>
        <p>
          Graylog let you enrich generated Events with dynamic values. You can access Fields from the Event context{' '}
          {/* eslint-disable-next-line no-template-curly-in-string */}
          with <code>{'${source.<fieldName>}'}</code>.
          <br />
          Available Fields in the Template depend on the condition that created the Event:
        </p>
        <ul>
          <li><b>Filter:</b> All Fields in the original log message</li>
          <li><b>Aggregation:</b> Fields set in Group By with their original names</li>
          <li><b>Correlation:</b> All Fields in the last matched and non-negated Event</li>
        </ul>
      </div>
    );
  }
}

export default TemplateFieldValueProviderPreview;
