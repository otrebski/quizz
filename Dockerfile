FROM alpine

RUN apk add npm
RUN npm --version
COPY ./gui/ /workdir
WORKDIR /workdir
RUN npm install && npm run-script build

FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1
COPY --from=0 /workdir/build /workdir/src/main/resources/gui
COPY ./ /workdir/
WORKDIR /workdir
RUN sbt universal:packageZipTarball

FROM openjdk:8
EXPOSE 8080:8080
COPY --from=1 /workdir/target/universal/quizz-0.2.tgz /
RUN tar xzvf /quizz-0.2.tgz
ENTRYPOINT ["quizz-0.2/bin/quizz"]
CMD []
