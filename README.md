# simple-cms

A simple web content management system using Compojure, Enlive and Twitter Bootstrap.

## Usage

The software is available on github: <https://github.com/ray1729/simple-cms>. 

It was written with my own website and workflow in mind, so is
unlikely to be useful to you out of the box. At the very least you
will have to update the layout template, and this may require changes
to the Enlive snippets in <code>views.clj</code>.

The basic idea is that the site content is managed in a separate git
repository. This should contain two directories, <code>content</code>
and <code>static</code>. Files in the <code>static</code> directory
are served verbatim. For example, the file
<code>$SITE_DIR/static/some/path.png</code> can be accessed from the
server via <code>/some/path.png</code>.

Items in <code>$SITE_DIR/content/some/path.html</code> will appear on
the server at <code>/content/some/path</code> (note: no
<code>.html</code> suffix). These articles are simple HTML with some
extra meta tags in the header, for example:

<pre>
  &lt;html&gt;
    &lt;head&gt;
      &lt;title&gt;Book Review - Secure Coding: Principles and Practices&lt;/title&gt;
      &lt;meta name="author" content="Ray Miller" /&gt;
      &lt;meta name="tags" content="book review, programming, security" /&gt;
      &lt;meta name="pubdate" content="2004-06-01" /&gt;
    &lt;/head&gt;
    &lt;body&gt;
      &lt;p class="teaser"&gt;
        This slim volume contains a wealth of information that will be of
        interest not only to software developers, but to anyone
        responsible for the deployment and operation of computer systems.
        Indeed, if I had one complaint about the book it would be that the
        title is misleading: it is not so much about secure coding as the
        overall software development process, and relatively little is
        said about coding per se. But do not let this put you off,
        programmers will benefit from reading this book too.
      &lt;/p&gt;
    &lt;/body&gt;
  &lt;/html&gt;
</pre>

The <code>title</code>, <code>author</code> and <code>pubdate</code>
become the header and subheader of the displayed item. The
<code>tags</code> are used to build the tag cloud and category index.
Paragraphs with class <code>teaser</code> will show up in the indexes.
Items without a <code>pubdate</code> will not appear in any indexes,
but can be accessed via <code>/preview/some/path</code>.

The CMS software takes care of rendering the article lists (optionally
filtered on tag) and the articles themselves. It also produces an Atom
feed for the latest articles (again, optionally filtered by tag).

The software provides no editing facilities. It is assumed that you
like to edit your content offline and commit to a version control
system. With this in mind, it also implements
<code>/api/refresh-site-content</code> which handles POST requsets
containing an API key. If the key is valid, a git pull and metadata
rebuild is initiated, updating the site content. This will usually be
triggered by a post-commit hook in your VCS.

The content paths and API key are configured
in <code>resources/simple-cms.properties</code>.

## Demo site

I will shortly be using this software to host my own website,
<http://www.1729.org.uk/>.

## License

Copyright (C) 2012 Ray Miller.

Distributed under the Eclipse Public License, the same as Clojure.
