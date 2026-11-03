# Correções de segurança do Alpine publicadas depois da imagem base entram em cada build.
FROM nginx:1.29-alpine@sha256:5616878291a2eed594aee8db4dade5878cf7edcb475e59193904b198d9b830de
RUN apk upgrade --no-cache
