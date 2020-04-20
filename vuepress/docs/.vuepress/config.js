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
      title: 'd4s',
      description: 'Dynamo DB Database Done Scala way',
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
            title: 'd4s',
            collapsable: false,
            sidebarDepth: 2,
            children: [
              '',
            ]
          }]
        }
      }
    },
  }
}