package top.ceclin.jser.tool

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

typealias Payload = ByteArray
typealias Gadget = Any
typealias ExceptionGadget = Exception

object PayloadOfGadget {
    operator fun invoke(gadget: Gadget): Payload = ByteArrayOutputStream().also {
        ObjectOutputStream(it).writeObject(gadget)
    }.toByteArray()
}
