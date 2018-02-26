package ffr

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import purecsv.safe.CSVReader


import scala.util.{Failure, Success, Try}

class FFR(rawData : List[String]) extends StrictLogging {

  type Result[A] = Either[String, A]

  // would put this in main but easier to test like this
  implicit val pwrError : Long = ConfigFactory.load().getLong("power.error")

  private def seconds(sec : Long) = sec*1000

  private def minutes(min: Long) = min *1000 * 60


  def parseCSV(): Result[List[RawEnergyRecord]] = {
    import ffr.ImplicitCSVConversion._

    val read = CSVReader[RawEnergyRecord].readCSVFromString(rawData.map(_.replaceAll("[\\$#]", "")).mkString("\n"))

    read.collectFirst {
      case Failure(ex) => {
        Left(ex.getMessage)
      }
    }.getOrElse(Right(read.flatMap(_.toOption)))
  }

  def addPower(energyRow: List[RawEnergyRecord]) : Result[List[ProcessedRecord]] = {

    Try {
      energyRow.sliding(2)
        .map(_.toList)
        .zipWithIndex
        .flatMap {
          case (left :: right :: Nil, index) =>
            val totalPhase = right.phaseOne + right.phaseTwo + right.phaseThree
            val previousPhase = left.phaseTwo + left.phaseOne + left.phaseThree
            val timeDiff = (right.dateTime.getMillis - left.dateTime.getMillis) / 1000.0

            val processedRecord = ProcessedRecord(
              right.dateTime.getMillis,
              right.frequency, totalPhase,
              right.relayStatus,
              Try { ((totalPhase - previousPhase) / timeDiff).toLong}.toOption
            )


            if (index == 0)
              List(ProcessedRecord(left.dateTime.getMillis, left.frequency, previousPhase, left.relayStatus, None), processedRecord)
            else
              List(processedRecord)
          case _ => throw new Exception("Unexpected error")
        }.toList
    } match {
      case Success(d) => Right(d)
      case Failure(ex) => Left(ex.getMessage)
    }
  }

  def trim(processedRecords: List[ProcessedRecord]): Result[List[ProcessedRecord]] = {

    def isTest(s: ProcessedRecord): Boolean = s.frequency > 49.99 && s.frequency < 50.01

    def loop(rows: List[ProcessedRecord],
             testInjection: Option[ProcessedRecord],
             count: Int): Result[List[ProcessedRecord]] = {
      (rows, testInjection, count) match {
        case (Nil, _, _) => {
          Left("No Test Data Found")
        }
        case (xs, _, 10) => Right(xs)
        case (x :: xs, _, _) if !isTest(x) => loop(xs, None, 0)
        case (x :: xs, Some(t), c) if t.frequency == x.frequency => loop(xs, Some(t), c + 1)
        case (x :: xs, _, _) => loop(xs, Some(x), 1)
      }
    }

    val afterTestData = loop(processedRecords, None, 0)

    val startTime = afterTestData.right.get.head.time

    afterTestData.map {
      _.takeWhile(e => (e.time - startTime) < minutes(35)).dropWhile(_.frequency > 49.7)
    }
  }

  def relaySwitchFastEnough(energyData : List[ProcessedRecord]) : Result[List[ProcessedRecord]] = {
    val relayReactionTime = energyData.head.time + 4000

    energyData
      .find(e => e.relayStatus == "on" && e.time < relayReactionTime)
      .map { s =>
        logger.info(s"It took ${s.time-(relayReactionTime-4000)} milliseconds to fire the relay switch!")
        energyData.dropWhile(_ != s)
      }
      .map(Right(_))
      .getOrElse(Left("Relay switch did not fire within 400ms!"))
  }

  // added a power error after actually running the data, was not including in test

  def powerDown(energyData: List[ProcessedRecord])
               (implicit pwrError : Long): Result[List[ProcessedRecord]] = {

    val firstTime = energyData.head.time

    val startPower = energyData.head.powerOutput.orElse(energyData.tail.head.powerOutput)

    val error = startPower.get * (pwrError/100.0)

    val (firstThirtySeconds, remainingTime) = energyData.span(_.time <  firstTime + 1000 * 30)

    logger.info(s"The orginal power was $startPower kW")
    logger.info(s"The power after 30 seconds is ${firstThirtySeconds.last.powerOutput} kW")

    energyData.find(r => r.powerOutput.exists(_==0L))
      .map(s => s.time).foreach{ time =>
      logger.info(s"Took ${(time-firstTime) / 1000} seconds to power Down!")
    }


    if (remainingTime.takeWhile(_.time < firstTime + minutes(30)+ seconds(30))
      .forall(r => r.powerOutput.forall { p => (p < 0L + error) && (p > 0L - error)}))
      Right(remainingTime.dropWhile(_.time < firstTime + minutes(30) + seconds(30)))

    else {
      Left("Did not remain powered down or did not power down fast enough")
    }
  }

  def powerUp(energyData: List[ProcessedRecord])
             (implicit pwrError : Long): Result[List[ProcessedRecord]] = {


    if(energyData.forall(r => r.powerOutput.forall(_>pwrError)))
      Right(energyData)
    else {
      logger.info(s"The average power after thirty minutes is ${energyData.flatMap(_.powerOutput).sum / energyData.length} kW")
      Left("Device did not power up!")
    }
  }

  // would add custom exceptions
  def run() : Result[Boolean] =
    parseCSV()
      .flatMap(addPower)
      .flatMap(trim)
      .flatMap(relaySwitchFastEnough)
      .flatMap(powerDown)
      .flatMap(powerUp)
      .map(_ => true) match {
      case Left(message) =>
        logger.error(message)
        logger.info("Did not meet requirements of FFR")
        Left(message)
      case r => r
    }

}