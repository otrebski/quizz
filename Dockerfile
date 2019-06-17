FROM hseeberger/scala-sbt

COPY ./ /workdir/
WORKDIR /workdir
RUN sbt universal:packageZipTarball

FROM openjdk:8
EXPOSE 8080:8080
COPY --from=0 /workdir/target/universal/quizz-0.1.tgz /
RUN ls -la /
RUN ls -la
RUN tar xzvf /quizz-0.1.tgz
RUN ls -la
ENTRYPOINT ["quizz-0.1/bin/quizz"]
CMD []
