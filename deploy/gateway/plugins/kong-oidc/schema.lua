local typedefs = require "kong.db.schema.typedefs"

return {
  name = "kong-oidc",
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    { config = {
      type = "record",
      fields = {
        { discovery = { type = "string", required = false } },
        { client_id = { type = "string", required = false } },
        { client_secret = { type = "string", required = false } },
        { redirect_uri = { type = "string", required = false } },
        { scope = { type = "string", required = false, default = "openid profile email" } },
      }
    } },
  }
}
