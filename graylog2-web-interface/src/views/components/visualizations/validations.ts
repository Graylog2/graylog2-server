import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

export const areAtLeastNMetricsConfigured = (formValues: WidgetConfigFormValues, minimumMetrics: number) => formValues.metrics?.length >= minimumMetrics;
export const areAtLeastNGroupingsConfigured = (formValues: WidgetConfigFormValues, minimumGroupings: number) => formValues.groupBy?.groupings?.length >= minimumGroupings;

export const hasAtLeastOneMetric = (name: string) => (formValues: WidgetConfigFormValues) => (!areAtLeastNMetricsConfigured(formValues, 1)
  ? { type: `${name} requires at least one metric` }
  : {});
