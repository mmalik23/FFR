package ffr

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import purecsv.safe.converter.StringConverter

import scala.util.Try

object ImplicitCSVConversion {

  implicit val dateTimeStringConverter = new StringConverter[DateTime] {
    override def tryFrom(str: String): Try[DateTime] = {
      val fmt = DateTimeFormat.forPattern("YYYY-MM-DD HH:mm:ss:SSS")

      Try(fmt.parseDateTime(str))
    }

    override def to(dateTime: DateTime): String = {
      dateTime.toString()
    }
  }

}
