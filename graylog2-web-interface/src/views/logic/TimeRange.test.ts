import type { TimeRangeQueryParameter } from 'views/logic/TimeRange';
import { timeRangeToQueryParameter, timeRangeFromQueryParameter } from 'views/logic/TimeRange';
import type { RelativeTimeRange, AbsoluteTimeRange, KeywordTimeRange } from 'views/logic/queries/Query';

describe('TimeRange', () => {
  describe('timeRangeToQueryParameter', () => {
    it('handles relative time ranges', () => {
      expect(timeRangeToQueryParameter({ type: 'relative', range: 300 }))
        .toEqual({ rangetype: 'relative', relative: '300' });

      expect(timeRangeToQueryParameter({ type: 'relative', from: 300 }))
        .toEqual({ rangetype: 'relative', from: '300' });

      expect(timeRangeToQueryParameter({ type: 'relative', from: 300, to: 150 }))
        .toEqual({ rangetype: 'relative', from: '300', to: '150' });

      expect(() => timeRangeToQueryParameter({ type: 'relative' } as RelativeTimeRange)).toThrowError(/Unexpected time range/);
    });

    it('handles absolute time ranges', () => {
      expect(timeRangeToQueryParameter({ type: 'absolute', from: '2022-08-03T11:52:00+01:00', to: '2022-08-04T11:52:00+01:00' }))
        .toEqual({ rangetype: 'absolute', from: '2022-08-03T11:52:00+01:00', to: '2022-08-04T11:52:00+01:00' });

      expect(timeRangeToQueryParameter({ type: 'absolute' } as AbsoluteTimeRange))
        .toEqual({ rangetype: 'absolute', from: undefined, to: undefined });
    });

    it('handles keyword time ranges', () => {
      expect(timeRangeToQueryParameter({ type: 'keyword', keyword: 'yesterday' }))
        .toEqual({ rangetype: 'keyword', keyword: 'yesterday' });

      expect(timeRangeToQueryParameter({ type: 'keyword' } as KeywordTimeRange))
        .toEqual({ rangetype: 'keyword', keyword: undefined });
    });
  });

  describe('timeRangeFromQueryParameter', () => {
    it('handles relative time ranges', () => {
      expect(timeRangeFromQueryParameter({ rangetype: 'relative', relative: '300' }))
        .toEqual({ type: 'relative', range: 300 });

      expect(timeRangeFromQueryParameter({ rangetype: 'relative', from: '300' }))
        .toEqual({ type: 'relative', from: 300 });

      expect(timeRangeFromQueryParameter({ rangetype: 'relative', from: '300', to: '150' }))
        .toEqual({ type: 'relative', from: 300, to: 150 });

      expect(() => timeRangeFromQueryParameter({ rangetype: 'relative' } as TimeRangeQueryParameter)).toThrowError(/Invalid relative range specified/);
    });

    it('handles absolute time ranges', () => {
      expect(timeRangeFromQueryParameter({ rangetype: 'absolute', from: '2022-08-03T11:52:00+01:00', to: '2022-08-04T11:52:00+01:00' }))
        .toEqual({ type: 'absolute', from: '2022-08-03T11:52:00+01:00', to: '2022-08-04T11:52:00+01:00' });

      expect(timeRangeFromQueryParameter({ rangetype: 'absolute', from: undefined, to: undefined }))
        .toEqual({ type: 'absolute' } as AbsoluteTimeRange);
    });

    it('handles keyword time ranges', () => {
      expect(timeRangeFromQueryParameter({ rangetype: 'keyword', keyword: 'yesterday' }))
        .toEqual({ type: 'keyword', keyword: 'yesterday' });

      expect(timeRangeFromQueryParameter({ rangetype: 'keyword', keyword: undefined }))
        .toEqual({ type: 'keyword' });
    });
  });
});
