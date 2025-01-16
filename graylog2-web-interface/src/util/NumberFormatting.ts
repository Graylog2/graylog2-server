type Options = {
  signDisplay?: 'auto' | 'always' | 'exceptZero',
  maximumFractionDigits?: number,
  minimumFractionDigits?: number,
};

const defaultOptions = {
  maximumFractionDigits: 2,
} as const;

const defaultPercentageOptions = {
  ...defaultOptions,
  style: 'percent',
} as const;

export const formatNumber = (num: number, options: Options = {}) => new Intl.NumberFormat(undefined, { ...defaultOptions, ...options }).format(num);
export const formatPercentage = (num: number, options: Options = {}) => new Intl.NumberFormat(undefined, { ...defaultPercentageOptions, ...options }).format(num);

type TrendOptions = {
  percentage?: boolean,
}
export const formatTrend = (num: number, options: TrendOptions = {}) => (options.percentage === true ? formatPercentage : formatNumber)(num, { signDisplay: 'exceptZero' });
