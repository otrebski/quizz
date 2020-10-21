package quizz.data

import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import better.files.File

class FileMindmupStoreTest extends AnyFlatSpec with Matchers {

  "FileMindmupStoreTest" should "save mindmup" in {
    val dir = File.newTemporaryDirectory()
    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.store("n", "content")
    } yield ()).unsafeRunSync()
    (dir / "n").contentAsString shouldBe "content"
  }

  "FileMindmupStoreTest" should "update mindmup" in {
    val dir = File.newTemporaryDirectory()
    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.store("n", "content")
    } yield ()).unsafeRunSync()
    (dir / "n").contentAsString shouldBe "content"

    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.store("n", "something else")
    } yield ()).unsafeRunSync()
    (dir / "n").contentAsString shouldBe "something else"
  }

  "FileMindmupStoreTest" should "delete mindmup" in {
    val dir = File.newTemporaryDirectory()
    (dir / "n").write("content")
    (dir / "n").exists shouldBe true

    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.delete("n")
    } yield ()).unsafeRunSync()
    (dir / "n").exists shouldBe false
  }

  "FileMindmupStoreTest" should "list all mindmups" in {
    val dir = File.newTemporaryDirectory()
    (dir / "n1").write("content")
    (dir / "n2").write("content")
    (dir / "n3").write("content")

    val names = (for {
      store <- FileMindmupStore[IO](dir)
      names <- store.listNames()
    } yield names).unsafeRunSync()

    names shouldBe Set("n1", "n2", "n3")
  }

  "FileMindmupStoreTest" should "load mindmup" in {
    val dir = File.newTemporaryDirectory()
    (dir / "n1").write("content")
    (dir / "n2").write("content")
    (dir / "n3").write("content")

    val store = FileMindmupStore[IO](dir)
    val content = (for {
      store   <- FileMindmupStore[IO](dir)
      content <- store.load("n1")
    } yield content).unsafeRunSync()

    content shouldBe "content"
  }

}
