package tree.data

import better.files.File
import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FileMindmupStoreTest extends AnyFlatSpec with Matchers {

  "FileMindmupStore" should "save mindmup" in {
    val dir = File.newTemporaryDirectory()
    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.store("n", "content")
    } yield ()).unsafeRunSync()
    (dir / "n" / "1").contentAsString shouldBe "content"
  }

  "FileMindmupStore" should "update mindmup" in {
    val dir = File.newTemporaryDirectory()
    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.store("n", "content")
    } yield ()).unsafeRunSync()
    (dir / "n" / "1").contentAsString shouldBe "content"

    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.store("n", "something else")
    } yield ()).unsafeRunSync()
    (dir / "n" / "1").contentAsString shouldBe "content"
    (dir / "n" / "2").contentAsString shouldBe "something else"
  }

  "FileMindmupStore" should "delete mindmup" in {
    val dir     = File.newTemporaryDirectory()
    val mapFile = dir / "n" / "1"
    mapFile.parent.createDirectoryIfNotExists()
    mapFile.write("content")
    mapFile.exists shouldBe true

    (for {
      store <- FileMindmupStore[IO](dir)
      _     <- store.delete("n", 1)
    } yield ()).unsafeRunSync()
    mapFile.exists shouldBe false
  }

  "FileMindmupStore" should "list all mindmups" in {
    val dir = File.newTemporaryDirectory()
    (1 to 3).foreach { i =>
      (dir / s"n$i").createDirectoryIfNotExists()
      ((dir / s"n$i") / "1").write(s"content$i")
    }

    val names = (for {
      store <- FileMindmupStore[IO](dir)
      names <- store.listNames()
    } yield names).unsafeRunSync()

    names shouldBe Set("n1", "n2", "n3")
  }

  "FileMindmupStore" should "load mindmup" in {
    val dir = File.newTemporaryDirectory()
    (1 to 3).foreach { i =>
      (dir / s"n$i").createDirectoryIfNotExists()
      ((dir / s"n$i") / "1").write(s"content$i")
    }

    val content = (for {
      store   <- FileMindmupStore[IO](dir)
      content <- store.load("n1", 1)
    } yield content).unsafeRunSync()

    content shouldBe MindmupStore.Mindmup("n1", 1, "content1")
  }

}
