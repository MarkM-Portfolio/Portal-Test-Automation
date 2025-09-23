# Git - Jenkins Integration

This directory contains the scripting, tooling and data that is necessary for a successful integration between GitHub and Jenkins.

**Please note:** Everything that is necessary for setting webhooks etc. in GitHub will be done by the [GitHub Maintenance Tools](https://git.cwp.pnp-hcl.com/Team-Q/Github-Maintenance-Tools).

## Authentication Reverse Proxy

This services is used to ease the configuration of webhooks between GitHub and Jenkins. Due to security reasons, Jenkins only accepts incoming webhooks that are either authenticated by a valid `Crumb` or with `basic authentication`. Since GitHub is only providing a possibility to add a URL for a webhook, it would be necessary to include the authentication data into the url set in the webhook. To prevent such a use of authentication data, the `Authentication Reverse Proxy` will enrich any request that is proxied through it with the necessary credentials for Jenkins. Please refer to the (Authentication Proxy README.md)[./authentication-proxy/README.md] for further information.

## Jenkins Manager

To create the pipelines needed for executing PR-Checks, we use a scripting based on NodeJS.