package io.github.antonkw

import cats.effect.{ IO, Resource }
import cats.implicits._
import natchez.Trace.Implicits.noop
import skunk.{ Command, Query, Session, Strategy }
import skunk.implicits._
import weaver.{ Expectations, IOSuite, Log }

sealed abstract class Table(val title: String)

object Table {
  case object Events extends Table("event_history")
}

abstract class PostgresSuite[Context] extends IOSuite {
  def makeContext: Resource[IO, Session[IO]] => Context
  val affectedTables: List[Table] = List.empty

  type AuxRes       = Resource[IO, Session[IO]]
  override type Res = (Context, AuxRes)
  override def maxParallelism = 1
  override def sharedResource: Resource[IO, (Context, AuxRes)] =
    Session
      .pooled[IO]("0.0.0.0", 5432, "timescale", "eventstore", Some("p_318Jhs&2"), 2, strategy = Strategy.SearchPath)
      .map(sessionPool => (makeContext(sessionPool), sessionPool))

  /**
    * Test case with clean up. Deletion of all rows from tables listed in
    * `affectedTables` is performed after test case.
    */
  def testWithTearDown(testName: String): (Context => IO[Expectations]) => Unit =
    testAfterEach(_.use(s => affectedTables.map(t => sql"DELETE FROM #${t.title}".command).traverse_(s.execute)))(testName)

  def testWithCleanUpBeforeAndTearDown(testName: String): (Context => IO[Expectations]) => Unit =
    testWithSetUpAndTearDown(testName)(s => affectedTables.map(t => sql"DELETE FROM #${t.title}".command).traverse_(s.execute))

  /**
    * Test case with clean up. Deletion of all rows from tables listed in
    * `affectedTables` is performed after test case.
    * Logger is provided.
    */
  def loggedTestWithTearDown(testName: String): ((Context, Log[IO]) => IO[Expectations]) => Unit =
    loggedTestAfterEach(_.use(s => affectedTables.map(t => sql"DELETE FROM #${t.title}".command).traverse_(s.execute)))(testName)

  /**
    * Test case with configurable setup action. Deletion of all rows from tables
    * listed in `affectedTables` is performed after test case.
    *
    * @param testName Name of the test case
    * @param setUp Action to be performed before test case execution body.
    *              Setup action is just an abstraction over `Session[IO] => IO[Unit]`
    *              function. It can be created from `skunk.Command` with help of method
    *              `runWith` provided by `PostgresSuiteSyntax`. Moreover, there is
    *              the way to compose setup actions using `>>` method also provided by
    *              `PostgresSuiteSyntax`.
    */
  def testWithSetUpAndTearDown(
      testName: String
  )(
      setUp: Session[IO] => IO[Unit]
  )(
      run: Context => IO[Expectations]
  )(implicit postRunExpectations: Session[IO] => IO[Expectations] = _ => IO.unit.as(success)): Unit =
    testBeforeAfterEach(
      _.use(setUp),
      _.use(s => affectedTables.map(t => sql"DELETE FROM #${t.title}".command).traverse_(s.execute)),
      _.use(postRunExpectations)
    )(testName)(run)

  /**
    * Test case with configurable setup action. Deletion of all rows from tables
    * listed in `affectedTables` is performed after test case.
    * Logger is provided.
    *
    * @param testName Name of the test case
    * @param setUp Action to be performed before test case execution body.
    *              Setup action is just an abstraction over `Session[IO] => IO[Unit]`
    *              function. It can be created from `skunk.Command` with help of method
    *              `runWith` provided by `PostgresSuiteSyntax`. Moreover, there is
    *              the way to compose setup actions using `>>` method also provided by
    *              `PostgresSuiteSyntax`.
    */
  def loggedTestWithSetUpAndTearDown(
      testName: String
  )(setUp: Session[IO] => IO[Unit]): ((Context, Log[IO]) => IO[Expectations]) => Unit =
    loggedTestBeforeAfterEach(
      _.use(setUp),
      _.use(s => affectedTables.map(t => sql"DELETE FROM #${t.title}".command).traverse_(s.execute))
    )(testName)

  private def testAfterEach(after: AuxRes => IO[Unit]): String => (Context => IO[Expectations]) => Unit =
    testName => (run: Context => IO[Expectations]) => test(testName)(res => run(res._1).guarantee(after(res._2)))

  private def loggedTestAfterEach(after: AuxRes => IO[Unit]): String => ((Context, Log[IO]) => IO[Expectations]) => Unit =
    (testName: String) =>
      (run: (Context, Log[IO]) => IO[Expectations]) => test(testName)((res, log) => run(res._1, log).guarantee(after(res._2)))

  private def testBeforeAfterEach(
      before: AuxRes => IO[Unit],
      after: AuxRes => IO[Unit],
      afterExpect: AuxRes => IO[Expectations]
  ): String => (Context => IO[Expectations]) => Unit =
    testName =>
      run =>
        test(testName)(res =>
          (before(res._2) *> run(res._1).flatMap(e => afterExpect(res._2).map(_ and e))).guarantee(after(res._2))
        )

  private def loggedTestBeforeAfterEach(
      before: AuxRes => IO[Unit],
      after: AuxRes => IO[Unit]
  ): String => ((Context, Log[IO]) => IO[Expectations]) => Unit =
    (testName: String) =>
      (run: (Context, Log[IO]) => IO[Expectations]) =>
        test(testName)((res, log) => (before(res._2) *> run(res._1, log)).guarantee(after(res._2)))
}

object PostgresSuiteSyntax {
  type SessionConsumer[T] = Session[IO] => IO[T]

  implicit class CommandOps[A](command: Command[A]) {
    def runWith(entity: A): SessionConsumer[Unit] = _.prepare(command).use(_.execute(entity)).void
  }

  implicit class QueryOps[A, B](query: Query[A, B]) {
    def queryWithAndReturn(entity: A): SessionConsumer[B] = _.prepare(query).use(_.unique(entity))
  }

  implicit class SessionConsumerOps[A](sessionConsumer: SessionConsumer[A]) {
    def >>[B](another: SessionConsumer[B]): SessionConsumer[B] = { s => sessionConsumer(s) >> another(s) }

    def >>=[B](f: A => SessionConsumer[B]): SessionConsumer[B] = { s => sessionConsumer(s) >>= { f(_)(s) } }

    def void: SessionConsumer[Unit] = { s => sessionConsumer(s).as(()) }
  }
}
