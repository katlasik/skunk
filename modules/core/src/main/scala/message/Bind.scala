package skunk.proto.message

import scodec._
import scodec.codecs._


case class Bind(portal: String, statement: String, args: List[Option[String]])

object Bind {

  implicit val BindFrontendMessage: FrontendMessage[Bind] =
    FrontendMessage.tagged('B') {

      // String   - The name of the destination portal (an empty string selects the unnamed portal).
      // String   - The name of the source prepared statement (an empty string selects the unnamed
      //            prepared statement).
      // Int16    - The number of parameter format codes that follow (denoted C below). This can be
      //            zero to indicate that there are no parameters or that the parameters all use the
      //            default format (text); or one, in which case the specified format code is
      //            applied to all parameters; or it can equal the actual number of parameters.
      // Int16[C] - The parameter format codes. Each must presently be zero (text) or one (binary).
      // Int16    - The number of parameter values that follow (possibly zero). This must match the
      //            number of parameters needed by the query.
      //
      // Next, the following pair of fields appear for each parameter:
      //
      // Int32    - The length of the parameter value, in bytes (this count does not include
      //            itself). Can be zero. As a special case, -1 indicates a NULL parameter value.
      //            No value bytes follow in the NULL case.
      // Byten    - The value of the parameter, in the format indicated by the associated format
      //            code. n is the above length.
      //
      // After the last parameter, the following fields appear:
      //
      // Int16    - The number of result-column format codes that follow (denoted R below). This
      //            can be zero to indicate that there are no result columns or that the result
      //            columns should all use the default format (text); or one, in which case the
      //            specified format code is applied to all result columns (if any); or it can equal
      //            the actual number of result columns of the query.
      // Int16[R] - The result-column format codes. Each must presently be zero (text) or one
      //            (binary).

      val arg: Codec[Option[String]] =
        Codec(
          Encoder { (os: Option[String]) =>
            os match {
              case None    => int32.encode(-1)
              case Some(s) =>
                for {
                  data <- utf8.encode(s)
                  len  <- int32.encode(data.size.toInt / 8)
                } yield len ++ data
            }
          },
          null // :-\
        )

      (utf8z ~ utf8z ~ int16 ~ int16 ~ list(arg) ~ int16).contramap[Bind] { b =>
        b.portal ~  // The name of the destination portal
        b.statement ~ // The name of the source prepared statement
         0 ~ // The number of parameter format codes
          b.args.length ~ // The number of parameter values
          b.args ~ // args
          0 // The number of result-column format codes
      }

    }

}