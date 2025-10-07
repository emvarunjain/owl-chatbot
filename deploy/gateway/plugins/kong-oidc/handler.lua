local BasePlugin = require "kong.plugins.base_plugin"
local KongOidc = BasePlugin:extend()

KongOidc.PRIORITY = 1000
KongOidc.VERSION = "0.1.0"

function KongOidc:new()
  KongOidc.super.new(self, "kong-oidc")
end

function KongOidc:access(conf)
  -- Placeholder: pass-through. Replace with real OIDC flow using lua-resty-openidc.
  -- Example:
  -- local res, err = require("resty.openidc").authenticate({
  --   redirect_uri = conf.redirect_uri,
  --   discovery = conf.discovery,
  --   client_id = conf.client_id,
  --   client_secret = conf.client_secret,
  --   scope = conf.scope
  -- })
  -- if err then return kong.response.exit(401, { message = err }) end
end

return KongOidc
