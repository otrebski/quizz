FROM alpine

RUN apk add npm
RUN npm --version
COPY ./gui/ /workdir
WORKDIR /workdir
RUN npm install && npm run-script build

FROM nginx
COPY ./gui/docker/default.conf /etc/nginx/conf.d/
COPY --from=0 /workdir/build /usr/share/nginx/html



#FROM nginx
#COPY ./docker/default.conf /etc/nginx/conf.d/
#COPY ./build /usr/share/nginx/html
