module.exports = {
  base: '/d4s/',
  head: [
    ['link', {
      rel: 'icon',
      href: '/D4S_logo.svg'
    }]
  ],
  locales: {
    '/': {
      lang: 'en-US',
      title: 'D4S',
      description: '«Dynamo DB Database Done Scala way»',
    }
  },
  themeConfig: {
    logo: '/D4S_logo.svg',
    locales: {
      '/': {
        selectText: 'Language',
        label: 'English',
        nav: [{
            text: 'Documentation',
            link: '/docs/'
          },
          {
            text: 'Resources',
            link: '/resources/'
          },
          {
            text: 'About',
            link: '/about/'
          },
          {
            text: 'Github',
            link: 'https://github.com/PlayQ/d4s'
          },
        ],
        sidebar: {
          '/docs/': [{
            title: 'D4S',
            collapsable: false,
            sidebarDepth: 2,
            children: [
              '',
              'tutorial',
              'setup',
              'table-definition',
              'basic-queries',
              'batched-operations',
              'conditionals',
              'indexes'
            ]
          }]
        }
      }
    },
  }
}