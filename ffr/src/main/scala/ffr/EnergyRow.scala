package ffr

import org.joda.time.DateTime

sealed case class EnergyRow(dateTime : DateTime,
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
                           )



