package ffr

import org.joda.time.DateTime
import org.scalatest.{FlatSpecLike, Matchers}

import scala.io.Source

class FFRSpec extends FlatSpecLike with Matchers {

  // I would add alot more testing around validation but its not really necessary for this project

  trait TestFixture {

    def readResource(path: String) : Array[String] = Source.fromResource(path).getLines.toArray

    val dateTime = DateTime.parse("2016-10-20 00:00:00:000")

    val dateTimeAsString = "2016-10-20 00:00:00:000"

    val goodRecord = s"#$dateTimeAsString,50.0689,0,0,0,0,1469345,1463552,1431758,0,1,off $"
    val goodEnergyRecord = EnergyRow(
      dateTime,
      "50.0689".toDouble,
      0, 0, 0, 0,
      1469345, 1463552, 1431758,
      0, 1,
      "off$")

    val injectedData: Array[String] = readResource("/testData/TestInjection")
  }



  "validator" should "validate csv" in new TestFixture {
    val badCSV = Array("terrible CSV MATE!")

    val goodCSV = Array(goodRecord)
    val expectedGoodRecords = Array(goodEnergyRecord)

    a [Exception] should be thrownBy{
      new FFR(badCSV).parseCSV()
    }

    new FFR(goodCSV).parseCSV() should contain theSameElementsAs expectedGoodRecords

  }



  "processor" should "fail since relay switch was not hit in 400ms" in new TestFixture {

    val failedToSwitchRelay = readResource("/testData/FailedToSwitchRelay.csv")

    val outOfBoundsRecord = Array("$2016-10-22 00:00:40:000,50.0689,0,0,0,0,1469345,1463552,1431758,0,1,off$")

    val csv = injectedData ++ outOfBoundsRecord ++ failedToSwitchRelay

    val ffr = new FFR(csv)

    ffr.run() shouldBe false

  }

  "processor" should "fail since the power did not go down in 30 seconds!!! " in  new TestFixture {

   val failedToPowerDown = readResource("/testData/FailedToPowerDown")

    val csv = injectedData ++ failedToPowerDown

    new FFR(csv).run() shouldBe false

  }

  "processor" should "fail since the power did not remain low for 30 mins!" in new TestFixture {

    val failedToKeepPowerDown = readResource("/testData/FailedToKeepPowerDown")

    val csv = injectedData ++ failedToKeepPowerDown

    new FFR(csv).run() shouldBe false

  }

  "processer" should "succeed and meet the ffr requirements" in new TestFixture {

    val success = readResource("/testData/Success.csv")

    val csv = injectedData ++ success

    new FFR(csv).run() shouldBe true

  }


}
