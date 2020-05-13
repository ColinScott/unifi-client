# UniFi Client

A client for communicating with Ubiquiti UniFi controllers.

Not associated with Ubiquiti.

## Supported Actions

### Global

- **authenticate** Obtain authentication tokens from the controller. Required for all other actions.
- **logout** Invalidate the authentication token to prevent further use.
- **sites** Get a list of sites on the controller visible to the authenticated user.

### Per Site

- **networks** Get the networks associated with the site. Returns a subset of the API data only.
- **firewallGroups** Get the Firewall Groups, sets of ports or addresses that can be reused across multiple rules.
- **firewallRules** Get the rules for the UniFi firewall.