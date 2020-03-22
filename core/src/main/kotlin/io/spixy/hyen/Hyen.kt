package io.spixy.hyen

import java.beans.Introspector
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class Hyen {
    private lateinit var outputStream: FileOutputStream
    private lateinit var fileDescriptor: FileDescriptor
    private var maxId = 0L

    private val typeToInt = hashMapOf<Class<*>, Int>()
    private val intToType = hashMapOf<Int, Class<*>>()
    private val intToSpawner = hashMapOf<Int, Spawner>()

    fun open(db: File) {
        Random.nextInt()
        if(!db.exists()) {
            db.createNewFile()
        }

        val dataInputStream = DataInputStream(BufferedInputStream(db.inputStream()))
        while (dataInputStream.available() > 0) {
            val obj = fromDataInputStream(dataInputStream)
            handleObject(obj)
        }

        outputStream = FileOutputStream(db, true)
        fileDescriptor = outputStream.fd
    }

    fun registerType(clazz: Class<*>, code: Int) {
        typeToInt[clazz] = code
        intToType[code] = clazz
        intToSpawner[code] = Spawner(clazz.name)
    }

    fun write(obj: HyenRecord) {
        if(obj.id == -1L) {
            obj.id = maxId + 1
        }
        val toByteArray = toByteArray(obj)
        outputStream.write(toByteArray)
        fileDescriptor.sync()
        handleObject(obj)
    }

    private fun handleObject(obj: Any?) {
        if(obj is HyenRecord) {
            if(obj.id > maxId) {
                maxId = obj.id
            }
            println(obj)
        }
    }

    private fun toByteArray(obj: Any): ByteArray {
        val result = when(obj) {
            is Int -> {
                val byteArray = ByteArray(8)
                ByteBuffer.wrap(byteArray, 0, 4).putInt(-1)
                ByteBuffer.wrap(byteArray, 4, 4).putInt(obj)
                byteArray
            }
            is Long -> {
                val byteArray = ByteArray(12)
                ByteBuffer.wrap(byteArray, 0, 4).putInt(-2)
                ByteBuffer.wrap(byteArray, 4, 8).putLong(obj)
                byteArray
            }
            is CharSequence -> {
                val dataBytes = obj.toString().toByteArray(StandardCharsets.UTF_8)
                val byteArray = ByteArray(4 + 4 + dataBytes.size)
                ByteBuffer.wrap(byteArray, 0, 4).putInt(-3)
                ByteBuffer.wrap(byteArray, 4, 4).putInt(dataBytes.size)
                ByteBuffer.wrap(byteArray, 8, dataBytes.size).put(dataBytes)
                byteArray
            }
            else -> {
                val javaType = obj::class.java
                val typeId = typeToInt[javaType] ?: throw IllegalStateException("not registered type ${javaType.name}")
                val dataBytesArrayOutputStream = ByteArrayOutputStream()
                val dataBytes = DataOutputStream(dataBytesArrayOutputStream).use { dataOutputStream ->
                    for (propertyDescriptor in Introspector.getBeanInfo(obj.javaClass).propertyDescriptors) {
                        if(propertyDescriptor.readMethod != null && propertyDescriptor.name != "class" || obj is HyenRecord && propertyDescriptor.name == "id") {
                            val nameBytes = propertyDescriptor.name.toByteArray(StandardCharsets.UTF_8)
                            val valueBytes = toByteArray(propertyDescriptor.readMethod.invoke(obj))
                            dataOutputStream.writeShort(nameBytes.size)
                            dataOutputStream.write(nameBytes)
                            dataOutputStream.write(valueBytes)
                        }
                    }
                    dataBytesArrayOutputStream.toByteArray()
                }

                val resultArrayOutputStream = ByteArrayOutputStream()
                val result = DataOutputStream(resultArrayOutputStream).use { dataOutputStream ->
                    dataOutputStream.writeInt(typeId)
                    dataOutputStream.writeInt(dataBytes.size)
                    dataOutputStream.write(dataBytes)
                    resultArrayOutputStream.toByteArray()
                }
                result
            }
        }
        return result
    }

    private fun fromByteArray(byteArray: ByteArray): Any? {
        val dataInputStream = DataInputStream(ByteArrayInputStream(byteArray))
        return fromDataInputStream(dataInputStream)
    }

    private fun fromDataInputStream(dataInputStream: DataInputStream): Any {
        val type = dataInputStream.readInt()
        return when(type) {
            -1 -> {
                dataInputStream.readInt()
            }
            -2 -> {
                dataInputStream.readLong()
            }
            -3 -> {
                val stringSize = dataInputStream.readInt()
                val stringBytes = dataInputStream.readNBytes(stringSize)
                stringBytes.toString(StandardCharsets.UTF_8)
            }
            else -> {
                val spawner = intToSpawner[type] ?: throw IllegalStateException("not registered type $type")
                val dataBytesSize = dataInputStream.readInt()
                val oldAvailable = dataInputStream.available()
                val keyValueMap = hashMapOf<String, Any?>()
                while (dataInputStream.available() - oldAvailable + dataBytesSize > 0) {
                    val nameBytesSize = dataInputStream.readShort()
                    val name = dataInputStream.readNBytes(nameBytesSize.toInt()).toString(StandardCharsets.UTF_8)
                    val value = fromDataInputStream(dataInputStream)
                    keyValueMap[name] = value
                }

                spawner.spawn(keyValueMap)
            }
        }
    }

    class Spawner(val name: String) {
        private val clazz by lazy { Class.forName("$name" + "HyenRecordSpawner") }
        private val instance by lazy { clazz.newInstance() }
        private val method by lazy { clazz.getMethod("spawn", Map::class.java) }

        fun spawn(values: Map<String, Any?>): Any {
            return method.invoke(instance, values)
        }
    }
}