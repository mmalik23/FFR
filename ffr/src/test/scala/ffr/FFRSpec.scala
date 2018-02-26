package ffr

import org.joda.time.format.DateTimeFormat
import org.scalatest.{FlatSpecLike, Matchers}

import scala.io.Source

class FFRSpec extends FlatSpecLike with Matchers {

  // I would add alot more testing around validation but its not really necessary for this project

  trait TestFixture {

    def readResource(path: String) :List[String] = Source.fromResource(path).getLines.toList

    val fmt = DateTimeFormat.forPattern("YYYY-MM-DD HH:mm:ss:SSS")

    val dateTime = fmt.parseDateTime("2016-10-20 00:00:00:000")



    val goodRecord = "#2016-10-20 00:00:00:000,50.0689,0,0,0,0,1469345,1463552,1431758,0,1,off$"
    val goodEnergyRecord = RawEnergyRecord(
      dateTime,
      "50.0689".toDouble,
      0, 0, 0, 0,
      1469345, 1463552, 1431758,
      0, 1,
      "off")

    val injectedData: List[String] = readResource("TestData/TestInjection.csv")


  }



  "validator" should "validate csv" in new TestFixture {
    val badCSV = List("terrible CSV MATE!")

    val goodCSV = List(goodRecord)
    val expectedGoodRecords = Right(List(goodEnergyRecord))

    new FFR(badCSV).parseCSV().isLeft shouldBe true

    new FFR(goodCSV).parseCSV() shouldBe expectedGoodRecords

  }



  "processor relay" should "fail since relay switch was not hit in 400ms" in new TestFixture {

    val failedToSwitchRelay = readResource("TestData/FailedToSwitchRelay.csv")


    val outOfBoundsRecord = Array("$2016-10-22 00:00:40:000,50.0689,0,0,0,0,1469345,1463552,1431758,0,1,off$")

    val csv = injectedData ++ outOfBoundsRecord ++ failedToSwitchRelay

    val ffr = new FFR(csv)

    ffr.run() shouldBe Left("Relay switch did not fire within 400ms!")

  }

  "processor power down " should "fail since the power did not go down in 30 seconds!!! " in  new TestFixture {

   val failedToPowerDown = readResource("TestData/FailedToPowerDown.csv")


    val csv = injectedData ++ failedToPowerDown

    new FFR(csv).run() shouldBe Left("Did not remain powered down or did not power down fast enough")

  }

  "processor remain powered down" should "fail since the power did not remain low for 30 mins!" in new TestFixture {

    val failedToKeepPowerDown = readResource("TestData/FailedToKeepPowerDown.csv")

    val csv = injectedData ++ failedToKeepPowerDown

    new FFR(csv).run() shouldBe Left("Did not remain powered down or did not power down fast enough")

  }

  "processer success" should "succeed and meet the ffr requirements" in new TestFixture {

    val success = readResource("TestData/Success.csv")

    val csv = injectedData ++ success



    new FFR(csv).run() shouldBe Right(true)

  }


}
