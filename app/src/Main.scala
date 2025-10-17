import cats.effect.{IO, IOApp, Resource}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.ibm.mq.jms.{MQQueueConnectionFactory, MQQueue}
import javax.jms.{Connection, Session, MessageProducer, MessageConsumer, TextMessage}

trait MQAlgebra[F[_]]:
  def sendMessage(queueName: String, message: String): F[Unit]
  def receiveMessage(queueName: String): F[Option[String]]

case class MQConfig(
  host: String = "localhost",
  port: Int = 1414,
  channel: String = "DEV.APP.SVRCONN",
  queueManager: String = "QM1"
)

object MQClient:
  // Resource management for MQ connection
  def connectionResource(config: MQConfig): Resource[IO, Connection] =
    Resource.make(
      IO.blocking {
        val cf = new MQQueueConnectionFactory()
        cf.setHostName(config.host)
        cf.setPort(config.port)
        cf.setChannel(config.channel)
        cf.setQueueManager(config.queueManager)

        val conn = cf.createQueueConnection()
        conn.start()
        conn
      }
    )(conn => IO.blocking(conn.close()).handleErrorWith(_ => IO.unit))

  def sessionResource(conn: Connection): Resource[IO, Session] =
    Resource.make(
      IO.blocking(conn.createSession(false, Session.AUTO_ACKNOWLEDGE))
    )(session => IO.blocking(session.close()).handleErrorWith(_ => IO.unit))

  // Implementation of our algebra
  def make(config: MQConfig): Resource[IO, MQAlgebra[IO]] =
    for
      conn <- connectionResource(config)
      session <- sessionResource(conn)
    yield new MQAlgebra[IO]:
      def sendMessage(queueName: String, message: String): IO[Unit] =
        Resource.make(
            IO.blocking {
              val queue = new MQQueue(queueName)
              session.createProducer(queue)
            }
          )(producer => IO.blocking(producer.close()).handleErrorWith(_ => IO.unit))
          .use { producer =>
            IO.blocking {
              val textMsg = session.createTextMessage(message)
              producer.send(textMsg)
            }
          }

      def receiveMessage(queueName: String): IO[Option[String]] =
        Resource.make(
            IO.blocking {
              val queue = new MQQueue(queueName)
              session.createConsumer(queue)
            }
          )(consumer => IO.blocking(consumer.close()).handleErrorWith(_ => IO.unit))
          .use { consumer =>
            IO.blocking {
              Option(consumer.receive(5000)).collect {
                case txt: TextMessage => txt.getText
              }
            }
          }

object Main extends IOApp.Simple:
  val config = MQConfig()
  val testQueue = "DEV.QUEUE.1"

  val run: IO[Unit] =
    MQClient.make(config).use { mq =>
      for
        _ <- IO.println(s"🚀 Starting MQ demo...")

        // Send a message
        msg = "Hello MQ!"
        _ <- IO.println(s"📤 Sending: '$msg'")
        _ <- mq.sendMessage(testQueue, msg)
        _ <- IO.println("✅ Message sent!")

        // Receive the message
        _ <- IO.println(s"📥 Receiving from $testQueue...")
        received <- mq.receiveMessage(testQueue)
        _ <- received match
          case Some(text) => IO.println(s"✅ Received: '$text'")
          case None => IO.println("❌ No message received (timeout)")

        _ <- IO.println("🎉 Demo complete!")
      yield ()
    }.handleErrorWith { err =>
      IO.println(s"❌ Error: ${err.getMessage}") >>
      IO.println("\n💡 Make sure IBM MQ is running locally with default dev config")
    }