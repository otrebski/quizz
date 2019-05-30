FROM hseeberger/scala-sbt

COPY ./ /workdir/
WORKDIR /workdir
RUN sbt universal:packageBin

FROM openjdk:8
COPY --from=0 /workdir/target/docker/stage/opt/docker /workdir
EXPOSE 8080:8080
ENTRYPOINT ["/workdir/bin/quizz"]
CMD []
