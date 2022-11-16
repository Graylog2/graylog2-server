export const typeLinkMap = {
  dashboard: { link: 'DASHBOARDS_VIEWID' },
  search: { link: 'SEARCH_VIEWID' },
};

export const lastOpen = [
  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
    summary: 'summary',
    description: 'description dfdsfds sdfadfsa sdfsadf sad asdf dsaf sdaf das asdf adsf asdfeweqreqwreqw dc ',
  },
  {
    type: 'search',
    title: 'SearchTitle1',
    id: '1111',
  },
  {
    type: 'dashboard',
    title: 'DashboardTitle2',
    id: '1111',
  },

  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
    summary: 'summary',
    description: 'description dfdsfds sdfadfsa sdfsadf sad asdf dsaf sdaf das asdf adsf asdfeweqreqwreqw dc ',
  },

  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
    summary: 'summary',
    description: 'description dfdsfds sdfadfsa sdfsadf sad asdf dsaf sdaf das asdf adsf asdfeweqreqwreqw dc ',
  },
];
export const activities = [{
  timestamp: '2021-11-09T14:28:24+01:00',
  entityType: 'search',
  entityName: 'Bobs search',
  action: {
    actionUser: 'Bob',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '111',
},
{
  timestamp: '2022-11-09T14:28:24+01:00',
  entityType: 'dashboard',
  entityName: 'Emmas dashboard',
  action: {
    actionUser: 'Emma',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '222',
},
{
  timestamp: '2021-11-09T14:28:24+01:00',
  entityType: 'search',
  entityName: 'Bobs search',
  action: {
    actionUser: 'Bob',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '111',
},
{
  timestamp: '2022-11-09T14:28:24+01:00',
  entityType: 'dashboard',
  entityName: 'Emmas dashboard',
  action: {
    actionUser: 'Emma',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '222',
},
{
  timestamp: '2021-11-09T14:28:24+01:00',
  entityType: 'search',
  entityName: 'Bobs search',
  action: {
    actionUser: 'Bob',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '111',
},
];

export const DEFAULT_PAGINATION = {
  per_page: 5,
  page: 1,
  count: 0,
  total: 0,
};
