# Azure-mock

Mock for OIDC/Oauth2 flyt mot Azure.

Støtter authorization flow, client credentials & on behalf of.

Støtter både basic auth og signed JWT.

## Bruk
Pek din klients discovery url til host + port `/.well-known/openid-configuration`

## OIDC Authorization flow uten interaksjon
- Default vil man bli spurt om Bruker ID og navn i en form. Om man ønsker å omgå manuelt steg under innloggingen kan disse to settes som query-paramter på discovery urlen.

Eks; `/.well-known/openid-configuration?name=Gizmo%20The%20Cat&user_id=22046474256`