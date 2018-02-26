package ffr

import scala.io.Source

object Main extends App {

  val rawCSV = Source.fromResource("readings.csv").getLines.toList

  new FFR(rawCSV).run()


}
