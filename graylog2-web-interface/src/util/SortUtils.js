import moment from 'moment';
import naturalSort from 'javascript-natural-sort';

// sortOrder: "asc"|"desc"
export function sortByDate(d1, d2, sortOrder) {
  const effectiveSortOrder = sortOrder || 'asc';
  const d1Time = moment(d1);
  const d2Time = moment(d2);

  if (effectiveSortOrder === 'asc') {
    return (d1Time.isBefore(d2Time) ? -1 : d2Time.isBefore(d1Time) ? 1 : 0);
  } else {
    return (d2Time.isBefore(d1Time) ? -1 : d1Time.isBefore(d2Time) ? 1 : 0);
  }
}

export function naturalSortIgnoreCase(s1, s2, sortOrder) {
  const effectiveSortOrder = sortOrder || 'asc';
  return (effectiveSortOrder === 'asc' ? naturalSort(s1.toLowerCase(), s2.toLowerCase()) : naturalSort(s2.toLowerCase(), s1.toLowerCase()));
}
