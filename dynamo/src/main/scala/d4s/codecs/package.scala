package d4s

package object codecs {

  type D4SCodec[A] = D4SEncoder[A] with D4SDecoder[A]
}
