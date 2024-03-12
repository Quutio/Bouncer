package io.quut.bouncer.velocity.extensions

import java.nio.ByteBuffer
import java.util.UUID

internal fun UUID.toByteArray(): ByteArray
{
	val buffer: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
	buffer.putLong(this.mostSignificantBits)
	buffer.putLong(this.leastSignificantBits)
	return buffer.array()
}
