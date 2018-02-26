package ffr

import com.typesafe.config.ConfigFactory

import scala.io.Source

object Main extends App {

  val filename  =  ConfigFactory.load().getString("csv.name")

  val rawCSV = Source.fromResource(filename).getLines.toList

  new FFR(rawCSV).run()


}
