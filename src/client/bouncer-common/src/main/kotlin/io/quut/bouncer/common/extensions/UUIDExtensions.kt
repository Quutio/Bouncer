package io.quut.bouncer.common.extensions

import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.util.UUID

internal fun UUID.toByteArray(): ByteArray
{
	val buffer: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
	buffer.putLong(this.mostSignificantBits)
	buffer.putLong(this.leastSignificantBits)
	return buffer.array()
}

internal fun toUuid(bytes: ByteString): UUID = toUuid(bytes.asReadOnlyByteBuffer())

internal fun toUuid(bytes: ByteBuffer): UUID
{
	return UUID(bytes.getLong(), bytes.getLong())
}
