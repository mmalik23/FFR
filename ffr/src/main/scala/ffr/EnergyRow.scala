package ffr

import org.joda.time.DateTime


sealed trait Record

sealed case class RawEnergyRecord(dateTime : DateTime,
                            frequency : Double,
                            legacy : Int,
                            legacyTwo : Int,
                            legacyThree : Int,
                            legacyFour : Int,
                            phaseOne : Long,
                            phaseTwo : Long,
                            phaseThree : Long,
                            legacyFive : Int,
                            legacySix : Int,
                            relayStatus : String
                           ) extends Record

sealed case class ProcessedRecord(time : Long,
                         frequency : Double
                         ,totalPhase : Long
                         , relayStatus : String
                         , powerOutput :Option[Long]) extends Record





