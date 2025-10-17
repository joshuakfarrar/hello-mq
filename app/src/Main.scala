import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  val run: IO[Unit] = IO.println("hello world")