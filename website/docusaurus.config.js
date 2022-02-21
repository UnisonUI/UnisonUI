// @ts-check

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

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
          lastVersion: '1.0.0',
          includeCurrentVersion: true
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
        blogTitle: 'Docusaurus changelog',
        blogDescription:
          'Keep yourself up-to-date about new features in every release',
        blogSidebarCount: 'ALL',
        blogSidebarTitle: 'Changelog',
        routeBasePath: '/changelog',
        showReadingTime: false,
        postsPerPage: 20,
        archiveBasePath: null,
        authorsMapPath: 'authors.json',
        feedOptions: {
          type: 'all',
          title: 'Docusaurus changelog',
          description:
            'Keep yourself up-to-date about new features in every release',
          copyright: `Copyright © ${new Date().getFullYear()} Facebook, Inc.`,
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
          { to: '/changelog', label: 'Releases', position: 'left' },
          {
            type: 'docsVersionDropdown',
            position: 'right',
            dropdownActiveClassDisabled: true,
          },
          {
            href: 'https://github.com/UnisonUI/UnisonUI',
            label: 'GitHub',
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
                to: '/changelog',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/UnisonUI/UnisonUI',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} UnisonUI, Inc. Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
