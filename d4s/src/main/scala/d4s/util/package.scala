package d4s

package object util {

  def leftpadLongInvertNegative(long: Long): String = {
    val str = leftpadLong(long)
    if (long < 0) negateDigits(str) else str
  }

  def negateDigits(str: String): String = {
    str.map {
      case '0' => '9'
      case '1' => '8'
      case '2' => '7'
      case '3' => '6'
      case '4' => '5'
      case '5' => '4'
      case '6' => '3'
      case '7' => '2'
      case '8' => '1'
      case '9' => '0'
      case chr => chr
    }
  }

  def negateMinus(str: String): String = {
    str.replace('-', 'Z')
  }

  def leftpadLong(long: Long): String = {
    long.formatted("%021d") // num padding in Long.MinValue
  }

}
