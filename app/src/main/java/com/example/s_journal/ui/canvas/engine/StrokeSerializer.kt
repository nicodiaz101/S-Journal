package com.example.s_journal.ui.canvas.engine

import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import com.example.s_journal.ui.canvas.model.SpenTool
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object StrokeSerializer {

    private const val MAGIC_HEADER = 0x534A494E // "SJIN"
    private const val FORMAT_VERSION = 1

    fun serialize(strokes: List<Stroke>): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteStream)

        dataOut.writeInt(MAGIC_HEADER)
        dataOut.writeInt(FORMAT_VERSION)
        dataOut.writeInt(strokes.size)

        for (stroke in strokes) {
            dataOut.writeUTF(stroke.id)
            val toolByte: Byte = when (stroke.tool) {
                SpenTool.PEN -> 0
                SpenTool.PENCIL -> 1
                SpenTool.HIGHLIGHTER -> 2
                SpenTool.ERASER -> 3
            }
            dataOut.writeByte(toolByte.toInt())
            dataOut.writeInt(stroke.color)
            dataOut.writeFloat(stroke.baseWidth)
            dataOut.writeInt(stroke.points.size)

            for (point in stroke.points) {
                dataOut.writeFloat(point.x)
                dataOut.writeFloat(point.y)
                dataOut.writeFloat(point.pressure)
                dataOut.writeLong(point.timestamp)
            }
        }

        dataOut.flush()
        return byteStream.toByteArray()
    }

    fun deserialize(bytes: ByteArray): List<Stroke> {
        if (bytes.isEmpty()) return emptyList()

        val byteStream = ByteArrayInputStream(bytes)
        val dataIn = DataInputStream(byteStream)

        val magic = dataIn.readInt()
        if (magic != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid .sjink magic header: 0x${Integer.toHexString(magic)}")
        }

        val version = dataIn.readInt()
        if (version != FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported .sjink format version: $version")
        }

        val strokeCount = dataIn.readInt()
        if (strokeCount < 0 || strokeCount > 500_000) {
            throw IllegalArgumentException("Invalid stroke count: $strokeCount")
        }
        val strokes = ArrayList<Stroke>(strokeCount.coerceAtMost(10_000))

        for (i in 0 until strokeCount) {
            val id = dataIn.readUTF()
            val toolByte = dataIn.readByte()
            val tool = when (toolByte.toInt()) {
                0 -> SpenTool.PEN
                1 -> SpenTool.PENCIL
                2 -> SpenTool.HIGHLIGHTER
                3 -> SpenTool.ERASER
                else -> SpenTool.PEN
            }
            val color = dataIn.readInt()
            val baseWidth = dataIn.readFloat()
            val pointCount = dataIn.readInt()
            if (pointCount < 0 || pointCount > 1_000_000) {
                throw IllegalArgumentException("Invalid point count: $pointCount")
            }

            val points = ArrayList<StrokePoint>(pointCount.coerceAtMost(10_000))
            for (j in 0 until pointCount) {
                val x = dataIn.readFloat()
                val y = dataIn.readFloat()
                val pressure = dataIn.readFloat()
                val timestamp = dataIn.readLong()
                points.add(StrokePoint(x = x, y = y, pressure = pressure, timestamp = timestamp))
            }

            strokes.add(
                Stroke(
                    id = id,
                    points = points,
                    color = color,
                    baseWidth = baseWidth,
                    tool = tool
                )
            )
        }

        return strokes
    }

    fun saveToFile(file: File, strokes: List<Stroke>) {
        file.parentFile?.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        val bytes = serialize(strokes)
        FileOutputStream(tempFile).use { it.write(bytes) }
        if (!tempFile.renameTo(file)) {
            // Fallback for filesystems that do not support atomic rename over existing file
            file.delete()
            if (!tempFile.renameTo(file)) {
                FileOutputStream(file).use { it.write(bytes) }
                tempFile.delete()
            }
        }
    }

    fun loadFromFile(file: File): List<Stroke> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        val bytes = FileInputStream(file).use { it.readBytes() }
        return deserialize(bytes)
    }
}
