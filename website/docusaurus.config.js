// @ts-check

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');
const versions = require('./versions.json');
const isDev = process.env.NODE_ENV === 'development';

function getNextBetaVersionName() {
  const expectedPrefix = '2.0.0-beta.';

  const lastReleasedVersion = versions[0];
  let version = 0;
  if (lastReleasedVersion.includes(expectedPrefix)) {
    version = parseInt(lastReleasedVersion.replace(expectedPrefix, ''), 10);
  }
  return `${expectedPrefix}${version + 1}`;
}

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'UnisonUI',
  tagline: "Unify all your service's specifications in one place",
  url: 'https://unisonui.tech/',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'UnisonUI',
  projectName: 'UnisonUI',
  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          lastVersion: isDev ? 'current' : versions[0],
          includeCurrentVersion: isDev,
          versions: isDev ? {
            current: {
              label: `${getNextBetaVersionName()} 🚧`,
            },
          } : {},
        },
        blog: {
          showReadingTime: true,
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
        gtag: {
          trackingID: 'G-E765F5MQF7',
          anonymizeIP: true,
        },
        sitemap: {
          changefreq: 'weekly',
          priority: 0.5,
        },
      }),
    ],
  ],
  plugins: [
    [
      require.resolve('./src/plugins/changelog/index.js'),
      {
        blogTitle: 'UnisonUI releases',
        blogDescription:
          'Keep yourself up-to-date about all releases',
        blogSidebarCount: 'ALL',
        blogSidebarTitle: 'Releases',
        routeBasePath: '/releases',
        showReadingTime: false,
        postsPerPage: 20,
        archiveBasePath: null,
        authorsMapPath: 'authors.json',
        feedOptions: {
          type: 'all',
          title: 'UnisonUI releases',
          description:
            'Keep yourself up-to-date about all releases',
          copyright: `MIT ${new Date().getFullYear()} UnisonUI. Built with Docusaurus.`,
          language: 'en',
        },
      },
    ],
  ],
  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'UnisonUI',
        logo: {
          alt: 'UnisonUI',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'doc',
            docId: 'getting-started/usage',
            position: 'left',
            label: 'Getting started',
          },
          { to: '/releases', label: 'Releases', position: 'left' },
          {
            type: 'docsVersionDropdown',
            position: 'right',
            dropdownActiveClassDisabled: true,
          },
          {
            href: 'https://github.com/UnisonUI/UnisonUI',
            className: 'header-github-link',
            'aria-label': 'GitHub repository',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Getting started',
                to: '/docs/getting-started/usage',
              },
              {
                label: 'Providers',
                to: '/docs/providers/git',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'Releases',
                to: '/releases',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/UnisonUI/UnisonUI',
              },
            ],
          },
        ],
        copyright: `MIT ${new Date().getFullYear()} UnisonUI. Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
      algolia: {
        appId: 'appId',
        apiKey: 'apiKey',
        indexName: 'unisonui',
      },
    }),
};

module.exports = config;
