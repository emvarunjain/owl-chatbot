FROM kong:3.6
USER root
RUN luarocks install lua-resty-openidc 1.7.5
# Copy community OIDC plugin into image (place your plugin code under plugins/kong-oidc)
COPY plugins/kong-oidc /usr/local/share/lua/5.1/kong/plugins/kong-oidc
ENV KONG_PLUGINS=bundled,kong-oidc
USER kong
