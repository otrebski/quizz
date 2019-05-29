# FROM alpine

# RUN apk add npm
# RUN npm --version
# COPY . /workdir
# WORKDIR /workdir
# RUN ls -la
# RUN npm install && npm run-script build
# RUN ls -la build

# FROM nginx
# COPY ./docker/default.conf /etc/nginx/conf.d/
# COPY --from=0 /workdir/build /usr/share/nginx/html



FROM nginx
COPY ./docker/default.conf /etc/nginx/conf.d/
COPY ./build /usr/share/nginx/html
