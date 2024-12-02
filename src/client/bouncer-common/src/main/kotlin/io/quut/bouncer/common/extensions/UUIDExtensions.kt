package io.quut.bouncer.common.extensions

import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.util.UUID

fun UUID.toByteArray(): ByteArray
{
	val buffer: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
	buffer.putLong(this.mostSignificantBits)
	buffer.putLong(this.leastSignificantBits)
	return buffer.array()
}

fun ByteString.toUuid(): UUID = this.asReadOnlyByteBuffer().getUuid()

fun ByteBuffer.getUuid(): UUID
{
	return UUID(this.getLong(), this.getLong())
}
