/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

import React from 'react';
import clsx from 'clsx';
import Translate from '@docusaurus/Translate';
import Link from '@docusaurus/Link';
import {
  PageMetadata,
  HtmlClassNameProvider,
  ThemeClassNames,
} from '@docusaurus/theme-common';
import BlogLayout from '@theme/BlogLayout';
import BlogListPaginator from '@theme/BlogListPaginator';
import SearchMetadata from '@theme/SearchMetadata';
import ChangelogItem from '@theme/ChangelogItem';
import type {Props} from '@theme/BlogListPage';

import styles from './styles.module.css';

function ChangelogListMetadata(props: Props): JSX.Element {
  const {metadata} = props;
  const {blogTitle, blogDescription} = metadata;
  return (
    <>
      <PageMetadata title={blogTitle} description={blogDescription} />
      <SearchMetadata tag="blog_posts_list" />
    </>
  );
}

function ChangelogListContent(props: Props): JSX.Element {
  const {metadata, items, sidebar} = props;
  const {blogTitle} = metadata;

  return (
    <BlogLayout sidebar={sidebar}>
      <header className="margin-bottom--lg">
        <h1 style={{fontSize: '3rem'}}>{blogTitle}</h1>
        <p>
          <Translate
            id="changelog.description"
            values={{
              rssLink: (
                <Link
                  href="pathname:///changelog/rss.xml"
                  className={styles.rss}>
                  <b>
                    <Translate id="changelog.description.rssLink">
                      RSS feeds
                    </Translate>
                  </b>
                  <svg
                    style={{
                      fill: '#f26522',
                      position: 'relative',
                      left: 4,
                      top: 1,
                      marginRight: 8,
                    }}
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24">
                    <path d="M6.503 20.752c0 1.794-1.456 3.248-3.251 3.248-1.796 0-3.252-1.454-3.252-3.248 0-1.794 1.456-3.248 3.252-3.248 1.795.001 3.251 1.454 3.251 3.248zm-6.503-12.572v4.811c6.05.062 10.96 4.966 11.022 11.009h4.817c-.062-8.71-7.118-15.758-15.839-15.82zm0-3.368c10.58.046 19.152 8.594 19.183 19.188h4.817c-.03-13.231-10.755-23.954-24-24v4.812z" />
                  </svg>
                </Link>
              ),
            }}>
            {
              'Subscribe through {rssLink} to stay up-to-date with new releases!'
            }
          </Translate>
        </p>
      </header>
      {items.map(({content: BlogPostContent}) => (
        <ChangelogItem
          key={BlogPostContent.metadata.permalink}
          frontMatter={BlogPostContent.frontMatter}
          assets={BlogPostContent.assets}
          metadata={BlogPostContent.metadata}
          truncated={BlogPostContent.metadata.truncated}>
          <BlogPostContent />
        </ChangelogItem>
      ))}
      <BlogListPaginator metadata={metadata} />
    </BlogLayout>
  );
}

export default function ChangelogList(props: Props): JSX.Element {
  return (
    <HtmlClassNameProvider
      className={clsx(
        ThemeClassNames.wrapper.blogPages,
        ThemeClassNames.page.blogListPage,
      )}>
      <ChangelogListMetadata {...props} />
      <ChangelogListContent {...props} />
    </HtmlClassNameProvider>
  );
}
