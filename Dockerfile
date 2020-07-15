FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1

COPY ./ /workdir/
WORKDIR /workdir
RUN sbt universal:packageZipTarball

FROM openjdk:8
EXPOSE 8080:8080
COPY --from=0 /workdir/target/universal/quizz-0.2.tgz /
RUN tar xzvf /quizz-0.2.tgz
ENTRYPOINT ["quizz-0.2/bin/quizz"]
CMD []
