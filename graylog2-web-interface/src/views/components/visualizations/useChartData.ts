import { useContext, useMemo } from 'react';
import type { Optional } from 'utility-types';

import type { ChartDataConfig } from 'views/components/visualizations/ChartData';
import { chartData } from 'views/components/visualizations/ChartData';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import UserDateTimeContext from 'contexts/UserDateTimeContext';

const useChartData = (rows: Rows, config: Optional<ChartDataConfig, 'formatTime'>) => {
  const { formatTime } = useContext(UserDateTimeContext);

  return useMemo(() => chartData(rows, {
    formatTime,
    ...config,
  }), [config, formatTime, rows]);
};

export default useChartData;
