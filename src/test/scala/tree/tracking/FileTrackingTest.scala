package tree.tracking

import java.time.Instant
import java.util.Date

import better.files.File
import cats.effect.IO
import cats.implicits.none
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FileTrackingTest extends AnyFlatSpec with Matchers {

  "FileTracking" should "list all sessions from file" in {
    val dir = File.newTemporaryDirectory()
    val sessions: List[TrackingSession] = (for {
      tracking <- FileTracking[IO](dir)
      _        <- tracking.step("q1", "p1", Instant.ofEpochMilli(100), "s1", none[String])
      _        <- tracking.step("q1", "p1:2", Instant.ofEpochMilli(400), "s1", none[String])
      _        <- tracking.step("q2", "p1:2:3", Instant.ofEpochMilli(300), "s1", none[String])
      sessions <- tracking.listSessions()
    } yield sessions).unsafeRunSync()
    sessions.toSet shouldBe Set(
      TrackingSession("s1", "q1", new Date(100), 300),
      TrackingSession("s1", "q2", new Date(300), 0)
    )
  }

  "FileTracking" should "list all no sessions if data is empty" in {
    val dir = File.newTemporaryDirectory()
    val sessions: List[TrackingSession] = (for {
      tracking <- FileTracking[IO](dir)
      sessions <- tracking.listSessions()
    } yield sessions).unsafeRunSync()
    sessions shouldBe List.empty
  }

  "FileTracking" should "list empty session if session or tree is incorrect" in {
    val dir = File.newTemporaryDirectory()
    val sessions: List[TrackingStep] = (for {
      tracking <- FileTracking[IO](dir)
      _        <- tracking.step("q1", "p1", Instant.ofEpochMilli(100), "s1", none[String])
      _        <- tracking.step("q1", "p1:2", Instant.ofEpochMilli(400), "s1", none[String])
      _        <- tracking.step("q2", "p1:2:3", Instant.ofEpochMilli(300), "s1", none[String])
      sessions <- tracking.session("incorrect", "incorrect")
    } yield sessions).unsafeRunSync()
    sessions shouldBe List.empty
  }

  "FileTracking" should "all steps for session" in {
    val dir = File.newTemporaryDirectory()
    val sessions: List[TrackingStep] = (for {
      tracking <- FileTracking[IO](dir)
      _        <- tracking.step("q1", "p1", Instant.ofEpochMilli(100), "s1", none[String])
      _        <- tracking.step("q1", "p1:2", Instant.ofEpochMilli(400), "s1", none[String])
      _        <- tracking.step("q2", "p1:2:3", Instant.ofEpochMilli(300), "s1", none[String])
      sessions <- tracking.session("s1", "q1")
    } yield sessions).unsafeRunSync()
    sessions shouldBe List(
      TrackingStep(0, "q1", "p1", new Date(100), "s1", none[String]),
      TrackingStep(0, "q1", "p1:2", new Date(400), "s1", none[String])
    )
  }

}
