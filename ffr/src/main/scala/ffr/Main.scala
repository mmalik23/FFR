package ffr


import com.typesafe.scalalogging.StrictLogging

import scala.io.Source


class FFR(rawData : Array[String]) {

  def parseCSV(): List[EnergyRow] = ???

  def orderDataByTimeStamp(parsedData: Seq[EnergyRow]) : List[EnergyRow] = ???

  def trimDataBeforeInjection(parsedData: Seq[EnergyRow]): List[EnergyRow] = ???

  def meetRequirementsOfFFR(energyData: Seq[EnergyRow]): Boolean = ???

  def run() : Boolean = ???

}

object Main extends App with StrictLogging {

  def apply(args : Array[String]) : Unit = {

    val rawCSV =
      if (args.isEmpty)
        Source.fromResource("resources.csv").getLines.toArray
      else if (args.length > 1) {
        logger.error("Too many program arguments")
        sys.exit(1)
      }
      else Source.fromResource(args(0)).getLines.toArray

  new FFR(rawCSV).run()
  }

}
