# -*- coding: utf-8 -*-
import os
import sys
import warnings
from datetime import date

from sphinx_scylladb_theme.utils import multiversion_regex_builder

sys.path.insert(0, os.path.abspath(".."))

# -- Global variables

# Build documentation for the following tags and branches
TAGS = ["v0.13.1", "v0.13.2"]
BRANCHES = ["develop"]
# Set the latest version.
LATEST_VERSION = "v0.13.2"
# Set which versions are not released yet.
UNSTABLE_VERSIONS = ["develop"]
# Set which versions are deprecated
DEPRECATED_VERSIONS = [""]

# -- General configuration ------------------------------------------------

# Add any Sphinx extension module names here, as strings.
extensions = [
    "sphinx.ext.autodoc",
    "sphinx.ext.todo",
    "sphinx.ext.mathjax",
    "sphinx.ext.githubpages",
    "sphinx.ext.extlinks",
    "sphinx_sitemap",
    "sphinx_scylladb_theme",
    "sphinx_multiversion",
    "myst_parser"  # for converting .md to .rst,
]

# The suffix(es) of source filenames.
source_suffix = [".rst", ".md"]

# The master toctree document.
master_doc = "index"

# General information about the project.
project = "Aerie"
copyright = str(date.today().year) + ", NASA. All rights reserved."
author = u"Aerie Project Contributors"

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
exclude_patterns = ["_build", "Thumbs.db", ".DS_Store"]

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = "sphinx"

# List of substitutions
rst_prolog = """
.. |rst| replace:: restructuredText
"""
# -- Options for not found extension ---------------------------------------

# Template used to render the 404.html generated by this extension.
notfound_template = "404.html"

# Prefix added to all the URLs generated in the 404 page.
notfound_urls_prefix = ""

# -- Options for sitemap extension ---------------------------------------

sitemap_url_scheme = "stable/{link}"

# -- Options for multiversion extension ----------------------------------

# Whitelist pattern for tags
smv_tag_whitelist = multiversion_regex_builder(TAGS)
# Whitelist pattern for branches
smv_branch_whitelist = multiversion_regex_builder(BRANCHES)
# Defines which version is considered to be the latest stable version.
smv_latest_version = LATEST_VERSION
smv_rename_latest_version = 'stable'  # Use the commit hash
# Whitelist pattern for remotes (set to None to use local branches only)
# smv_remote_whitelist = r"^origin$"
smv_remote_whitelist = r'^.*$'
# Pattern for released versions
smv_released_pattern = r"v\d+\.\d+\.\d+$"
# Format for versioned output directories inside the build directory
smv_outputdir_format = "{ref.name}"

# -- Options for HTML output ----------------------------------------------

# The theme to use for pages.
html_theme = "sphinx_scylladb_theme"
html_theme_path = ["../"]

# Theme options are theme-specific and customize the look and feel of a theme
# further.  For a list of options available for the theme, see the
# documentation.
html_theme_options = {
    "conf_py_path": "docs/source/",
    "hide_edit_this_page_button": "false",
    "hide_banner": "true",
    "github_issues_repository": "nasa-ammos/aerie",
    "github_repository": "nasa-ammos/aerie",
    "site_description": "Aerie documentation.",
    "hide_version_dropdown": [],
    "versions_unstable": UNSTABLE_VERSIONS,
    "versions_deprecated": DEPRECATED_VERSIONS,
}

# Last updated format
html_last_updated_fmt = "%d %b %Y"

# Custom sidebar templates, maps document names to template names.
html_sidebars = {"**": ["side-nav.html"]}

# Output file base name for HTML help builder.
htmlhelp_basename = "AerieDocumentationdoc"

# URL which points to the root of the HTML documentation.
html_baseurl = "https://nasa-ammos.github.io/aerie"

# Dictionary of values to pass into the template engine’s context for all pages
html_context = {"html_baseurl": html_baseurl}

# Ignore header warnings from MyST (Note: MyST will replace the headers)
suppress_warnings = ["myst.header"]
myst_heading_anchors = 6


# -- Initialize Sphinx ----------------------------------------------
def setup(sphinx):
    warnings.filterwarnings(
        action="ignore",
        category=UserWarning,
        message=r".*Container node skipped.*",
    )
